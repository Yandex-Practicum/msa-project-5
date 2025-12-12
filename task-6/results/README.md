## Important

⚠️ The solution is based on a result achieved in `task-5`.

## Changes
* add `micrometer` to tracing support (`build.gradle`);
* prepare tracing configuration;
* add `BatchJobController` for running a batch job through a `REST API` interface;
* adjust `BatchProcessingApplication` to ensure an app is running unless expicitly closed (e.g. by stopping a container);
* adjust `application.properties` (`management.tracing.sampling.probability` and `logging.pattern.level`);
* set a logging pattern in `logback-spring.xml`;
* create a `Python` client, which launches a batch job through the prepared `REST API`.

## Demo

A video with a proof of concept is available here: https://disk.yandex.ru/i/Wpq_WtOhel_hCQ