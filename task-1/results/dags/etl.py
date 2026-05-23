from airflow import DAG
from airflow.providers.http.operators.http import HttpOperator
from airflow.providers.standard.operators.python import PythonOperator, BranchPythonOperator
from datetime import date, datetime
import xmltodict
import json
import os

def transform_xml_to_json(**context):
    ti = context['ti']
    xml_data = ti.xcom_pull(task_ids='1_extract')
    print(xml_data)
    if not xml_data:
        return ValueError("No xml data received")
    
    data_dict = xmltodict.parse(xml_data)
    json_data = json.dumps(data_dict)
    return json_data

def analyze_json(**context):
    hour = datetime.now().hour
    if hour<12:
        return '3_load'
    else:
        return '3_1_reload'
    
def save_json_to_file(**context):
    ti = context['ti']
    json_data = ti.xcom_pull(task_ids='2_transform')
    
    if not json_data:
        return ValueError("No JSON data received")
    
    output_dir = '/opt/airflow/data'
    filename = 'output.json'
    
    path = os.path.join(output_dir, filename)
    
    os.makedirs(output_dir, exist_ok=True)
    
    with open(path, 'w') as f:
        f.write(json_data)
    print (f'JSON saved to {path}')
    
    return path
    
def reload_json_to_file(**context):
    ti = context['ti']
    json_data = ti.xcom_pull(task_ids='2_transform')
    
    if not json_data:
        return ValueError("No JSON data received")
    
    output_dir = '/opt/airflow/data'
    filename = 'output2.json'
    
    path = os.path.join(output_dir, filename)
    
    os.makedirs(output_dir, exist_ok=True)
    
    with open(path, 'w') as f:
        f.write(json_data)
    print (f'JSON saved to {path}')
    
    return path
            
default_args = {
    'owner': 'airflow',
    'start_date': datetime(2026,1,1),
    'retries': 3,
    'retry_delay': timedelta(minutes=5)
}
with DAG(
    dag_id = 'test_etl',
    description = 'For tests',
    default_args = default_args
) as dag:
    day = date.today().strftime('%d')
    month = date.today().strftime('%m')
    year = date.today().strftime('%Y')

    endpoint = f'/scripts/XML_daily.asp?date_req={day}/{month}/{year}'
    print(endpoint)
    extract = HttpOperator(
        task_id = '1_extract',
        http_conn_id ='cb_rf',
        method = 'GET',
        endpoint = endpoint,
        
    )
    transform = PythonOperator(
        task_id = "2_transform",
        python_callable = transform_xml_to_json)
        
    analyze = BranchPythonOperator(
        task_id = "2_1_analyze",
        python_callable = analyze_json)
    
    load = PythonOperator(
                   task_id = "3_load",
                   python_callable = save_json_to_file)
    
    reload = PythonOperator(
                       task_id = "3_1_reload",
                       python_callable = reload_json_to_file)
                   
    extract >> transform >> analyze
    analyze >> load
    analyze >> reload