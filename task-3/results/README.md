## Problem statement

An online platform needs to conduct data export on a daily basis at 8pm.
A development team decided to move forward with `k8s job`.

## Solution

`Python` is chosen as an ETL pipeline implementation language in order to speed up mvp development.

A set of configuration files:
* `postgre_sql_db.yaml` (spawns a seeded database containing a `users` table);
* `Dockerfile` (the backbone of an etl process);
* `etl.py` (the etl process itself);
* `requirements.txt` (dependencies of the etl process);
* `cron_job.yaml` (`k8s job` configuration).

## Result
A demo is available at https://disk.yandex.ru/i/jcE55ULKYf91VQ