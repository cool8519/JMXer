JMXer
=============================
일반적인 미들웨어의 경우, 기본적으로 Accesslog를 이용하여 클라이언트 요청 정보를 기록한다. 여기에는 모든 요청에 대한 처리결과가 기록되어 있으므로, 이것을 이용하여 시스템의 성능을 분석할 수 있다. 가령, PeakTime은 언제인지, 최대 몇 TPS를 처리하는지, 응답시간 지연이 얼마나 발생하며 어떤 요청이 주로 지연되는지, 오류비율은 얼마나 되는지 등을 Accesslog를 통해 분석이 가능하다. ALYBA는 Accesslog를 분석하여 시스템의 현황을 점검하고 더 나아가 악성 어플리케이션을 식별하기 위한 목적으로 개발되었다.

Requirements
---------------
* OS : Linux, Windows
* JAVA : JDK 6 이상

Getting Started
---------------
아래의 JAVA Archive 파일(jar)을 다운로드 한다.
* Java Archive File : [JMXer_v1.0.jar](https://github.com/cool8519/JMXer/blob/main/output/JMXer_v1.0.jar)

커맨드 창에서 아래와 같이 실행한다.
`> java -jar JMXer.jar`

일반적으로 프로세스에 Attach하는 방식으로 실행하나, 아래와 같이 프로세스 Attach 없이 덤프 분석모드로 실행할 수 있다.

`> java -jar ALYBA.jar DUMPFILE[=path_to_jmxer_dump_file]`

Usage
---------------


Release Note
--------------


To-do
--------------

