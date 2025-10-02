"""
Конфигурация для отправки email уведомлений.
Читает настройки из переменных окружения для безопасности.
"""

import os

# Настройки SMTP сервера из переменных окружения
SMTP_CONFIG = {
    'smtp_host': os.getenv('SMTP_HOST', 'smtp.gmail.com'),
    'smtp_port': int(os.getenv('SMTP_PORT', '587')),
    'smtp_username': os.getenv('SMTP_USER', ''),
    'smtp_password': os.getenv('SMTP_PASSWORD', ''),
    'smtp_use_tls': os.getenv('AIRFLOW__SMTP__SMTP_STARTTLS', 'True').lower() == 'true',
    'smtp_use_ssl': os.getenv('AIRFLOW__SMTP__SMTP_SSL', 'False').lower() == 'true',
}

# Email адреса для уведомлений из переменных окружения
EMAIL_RECIPIENTS = {
    'admin': os.getenv('EMAIL_ADMIN', 'admin@example.com'),
    'data_team': os.getenv('EMAIL_DATA_TEAM', 'data-team@example.com'),
    'dev_team': os.getenv('EMAIL_DEV_TEAM', 'dev-team@example.com'),
}

# Настройки уведомлений
NOTIFICATION_SETTINGS = {
    'send_success_emails': True,
    'send_failure_emails': True,
    'send_retry_emails': True,
    'email_template_dir': '/opt/airflow/templates/',
}

# HTML шаблоны для email
EMAIL_TEMPLATES = {
    'success_subject': '✅ Пакетная обработка данных завершена успешно',
    'failure_subject': '❌ Ошибка в пакетной обработке данных',
    'retry_subject': '⚠️ Повторная попытка выполнения задачи',
}

# Инструкции по настройке:
"""
1. Создайте файл env.local в корне проекта с вашими SMTP настройками:
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=587
   SMTP_USER=your_email@gmail.com
   SMTP_PASSWORD=your_app_password
   SMTP_MAIL_FROM=your_email@gmail.com
   EMAIL_ADMIN=admin@example.com
   EMAIL_DATA_TEAM=data-team@example.com
   EMAIL_DEV_TEAM=dev-team@example.com

2. Для Gmail:
   - Включите двухфакторную аутентификацию
   - Создайте App Password в настройках безопасности
   - Используйте App Password вместо обычного пароля

3. Для других провайдеров:
   - Проверьте настройки SMTP на сайте вашего провайдера
   - Обновите SMTP_HOST и SMTP_PORT в env.local

4. Перезапустите Docker Compose:
   docker-compose down
   docker-compose up -d

5. Убедитесь, что env.local добавлен в .gitignore для безопасности
"""
