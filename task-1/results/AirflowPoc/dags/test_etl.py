from airflow import DAG
from airflow.operators.python import PythonOperator, BranchPythonOperator
from airflow.operators.email import EmailOperator
from datetime import datetime, timedelta
import csv
import random
import time

def read_orders(**context):
    """ШАГ 1: Чтение данных из CSV (файловое хранилище)"""
    orders = []
    # Путь внутри Docker контейнера
    with open('/opt/airflow/data/orders.csv', 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            orders.append(row)
    
    high_value = sum(1 for o in orders if float(o['amount']) > 1000)
    context['ti'].xcom_push(key='high_value_count', value=high_value)
    
    print("="*50)
    print(f"📊 ШАГ 1: Чтение данных")
    print(f"✅ Прочитано {len(orders)} заказов")
    print(f"💰 Дорогих заказов (>1000): {high_value}")
    print("="*50)
    
    return high_value

def branch_by_value(**context):
    """ШАГ 2: ВЕТВЛЕНИЕ пайплайна по условию"""
    high_value = context['ti'].xcom_pull(task_ids='read_orders', key='high_value_count')
    
    print("="*50)
    print(f"🔄 ШАГ 2: Ветвление")
    print(f"📈 Количество дорогих заказов: {high_value}")
    
    if high_value > 2:
        print(f"➡️ Выбрана ветка: process_high_value")
        result = 'process_high_value'
    else:
        print(f"➡️ Выбрана ветка: process_standard")
        result = 'process_standard'
    print("="*50)
    
    # Сохраняем выбранную ветку в XCom
    context['ti'].xcom_push(key='selected_branch', value=result)
    
    return result

def process_high_value(**context):
    """Ветка 1: Обработка дорогих заказов (с ретраями)"""
    print("="*50)
    print("🚀 ШАГ 3: Расширенная обработка дорогих заказов")
    
    # Имитация случайной ошибки для демонстрации retry
    if random.random() < 0.3:  # 30% шанс ошибки
        error_msg = "Имитация временного сбоя внешнего API"
        print(f"❌ ОШИБКА: {error_msg}")
        print("🔄 Будет выполнена попытка повторного запуска (retry)")
        print("="*50)
        raise Exception(error_msg)
    
    # Имитация длительной обработки
    time.sleep(2)
    
    print("✅ Дорогие заказы успешно обработаны")
    print("📝 Выполнена расширенная валидация и обогащение данных")
    print("="*50)
    
    context['ti'].xcom_push(key='processing_result', value='high_value_success')
    context['ti'].xcom_push(key='processing_details', value='Обработано дорогих заказов: 3, сумма: 5200')
    context['ti'].xcom_push(key='execution_time', value=time.strftime('%Y-%m-%d %H:%M:%S'))
    
    return "high_value_success"

def process_standard(**context):
    """Ветка 2: Стандартная обработка"""
    print("="*50)
    print("📦 ШАГ 3: Стандартная обработка заказов")
    
    # Имитация обработки
    time.sleep(1)
    
    print("✅ Стандартные заказы успешно обработаны")
    print("📝 Выполнена базовая валидация данных")
    print("="*50)
    
    context['ti'].xcom_push(key='processing_result', value='standard_success')
    context['ti'].xcom_push(key='processing_details', value='Обработано стандартных заказов: 2, сумма: 1750')
    context['ti'].xcom_push(key='execution_time', value=time.strftime('%Y-%m-%d %H:%M:%S'))
    
    return "standard_success"

def prepare_success_email_content(**context):
    """Подготовка контента для успешного email"""
    ti = context['ti']
    
    # Получаем данные из XCom
    high_value_count = ti.xcom_pull(task_ids='read_orders', key='high_value_count')
    selected_branch = ti.xcom_pull(task_ids='branch_by_value', key='selected_branch')
    execution_time = ti.xcom_pull(task_ids=[selected_branch], key='execution_time')
    processing_details = ti.xcom_pull(task_ids=[selected_branch], key='processing_details')
       
    html_content = ''

    # Сохраняем контент в XCom
    ti.xcom_push(key='email_content', value=html_content)
    ti.xcom_push(key='email_subject', value='✅ ETL: Успешное выполнение пайплайна')
    
    return html_content

def prepare_failure_email_content(**context):
    """Подготовка контента для email об ошибке"""
    ti = context['ti']
    
    # Получаем данные из XCom
    high_value_count = ti.xcom_pull(task_ids='read_orders', key='high_value_count')
    selected_branch = ti.xcom_pull(task_ids='branch_by_value', key='selected_branch')
    
    # Определяем, какая задача упала
    failed_task = None
    error_message = "Неизвестная ошибка"
    
    try:
        # Пытаемся получить информацию об ошибке
        if selected_branch == 'process_high_value':
            task_instance = ti.get_task_instance('process_high_value')
            if task_instance.state == 'failed':
                failed_task = 'process_high_value'
                # В реальном сценарии здесь можно получить реальную ошибку
                error_message = "Ошибка при обработке дорогих заказов: превышен таймаут API"
        else:
            task_instance = ti.get_task_instance('process_standard')
            if task_instance.state == 'failed':
                failed_task = 'process_standard'
                error_message = "Ошибка при обработке стандартных заказов: неверный формат данных"
    except:
        failed_task = "Не удалось определить"
    
    html_content = ''
    
    # Сохраняем контент в XCom
    ti.xcom_push(key='email_content', value=html_content)
    ti.xcom_push(key='email_subject', value='❌ Ошибка в ETL пайплайне')
    
    return html_content

# Настройка пайплайна
default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 3,
    'retry_delay': timedelta(seconds=5),
}

with DAG(
    'demo_etl_with_email',
    default_args=default_args,
    start_date=datetime(2024, 1, 1),
    schedule=None,
    catchup=False,
    tags=['demo', 'etl', 'email'],
    description='ETL пайплайн с отправкой email',
) as dag:

    # ШАГ 1: Чтение данных
    read = PythonOperator(
        task_id='read_orders',
        python_callable=read_orders,
    )

    # ШАГ 2: Ветвление по условию
    branch = BranchPythonOperator(
        task_id='branch_by_value',
        python_callable=branch_by_value,
    )

    # ШАГ 3: Обработка сегментов
    high_value = PythonOperator(
        task_id='process_high_value',
        python_callable=process_high_value,
        retries=2,
        retry_delay=timedelta(seconds=5),
    )

    standard = PythonOperator(
        task_id='process_standard',
        python_callable=process_standard,
    )

    # ШАГ 4: Подготовка контента для email
    prepare_success = PythonOperator(
        task_id='prepare_success_email',
        python_callable=prepare_success_email_content,
        trigger_rule='all_done',
    )

    prepare_failure = PythonOperator(
        task_id='prepare_failure_email',
        python_callable=prepare_failure_email_content,
        trigger_rule='all_done',
    )

    # ШАГ 5: Отправка email
    send_success_email = EmailOperator(
        task_id='send_success_email',
        to=['buford.schinner@ethereal.email'],
        subject="""{{ ti.xcom_pull(task_ids='prepare_success_email', key='email_subject') }}""",
        html_content="""{{ ti.xcom_pull(task_ids='prepare_success_email', key='email_content') }}""",
        trigger_rule='all_success',
    )

    send_failure_email = EmailOperator(
        task_id='send_failure_email',
        to=['buford.schinner@ethereal.email'],
        subject="""{{ ti.xcom_pull(task_ids='prepare_failure_email', key='email_subject') }}""",
        html_content="""{{ ti.xcom_pull(task_ids='prepare_failure_email', key='email_content') }}""",
        trigger_rule='one_failed',
    )

# ========== ОРКЕСТРАЦИЯ ==========

# 1. Базовая цепочка
read >> branch >> [high_value, standard]

# 2. Подготовка email
high_value >> [prepare_success, prepare_failure]
standard >> [prepare_success, prepare_failure]

# 3. Отправка email
prepare_success >> send_success_email
prepare_failure >> send_failure_email