# 데이터 수집 개요
## 스마트폰
### 수집 데이터 항목
* **Physical**
    * Item: Human Activities (Walking, Running, ...)
    * Entries: Start Time, End Time, Duration
* **Digital**
    * Item: App Usage (facebook, youtube, ...)
    * Entries: Start Time, End Time, Duration
* **Social**
    * Item: Call Logs, SMS Logs
    * Entries: 
        * Call Logs: Start Time, End Time, Duration
        * SMS Logs: TBD
### 데이터 테이블 예시
| Date | Category | Name | Start Time | End Time | Duration(ms) |
| --- | --- | --- | --- | --- | --- |
| 2026-06-16 | DIGITAL | com.facebook.katana | 1.78162E+12 | 1.78162E+12 | 1739 |
| 2026-06-16 |	DIGITAL	| com.sec.android.app.shealth |	1.78162E+12	| 1.78162E+12 |	1375
| 2026-06-16 |	PHYSICAL	| WALKING |	1.78162E+12 | 	1.78162E+12 |	8246

## 스마트워치
* TBD

# 파이어베이스 연동 가이드
### 1. 파이어베이스 콘솔에 추가 안드로이드 앱 등록
* 파이어베이스 콘솔 접속: 기존에 사용하던 파이어베이스 프로젝트 접속 (https://console.firebase.google.com/u/1/project/hqs-exp/overview)
* 프로젝트 설정 이동: 좌측 메뉴 최상단에 있는 Settings -> General 접속
* 앱 추가: 설정 화면 하단의 Your apps 섹션 -> 'Add app' 클릭
* 패키지명 입력: 새로 만든 안드로이드 프로젝트의 정확한 패키지 이름(예: com.mycompany.secondapp)을 입력하고 'Register' 클릭.

### 2. 구성 파일 다운로드 및 적용
* 다운로드: 화면에 나타나는 (업데이트된) google-services.json 파일 다운로드
* 파일 덮어쓰기 (동시에 진행):
    * 새 프로젝트: 다운로드한 파일을 새 프로젝트 폴더 / app / 폴더 안에 바로 붙여넣기
    * 기존 프로젝트: 기존 안드로이드 프로젝트도 열어서, 예전에 넣어뒀던 google-services.json 파일을 지우고 방금 다운받은 최신 파일로 교체

### 3. 새 프로젝트에 파이어베이스 SDK 설치
* 프로젝트 수준의 build.gradle (또는 build.gradle.kts) 수정:
``` 
plugins {
    // 기존 플러그인들...
    id("com.google.gms.google-services") version "4.4.1" apply false
}
```

* 앱(모듈) 수준의 build.gradle (또는 build.gradle.kts) 수정:
```
plugins {
    // 기존 플러그인들...
    id("com.google.gms.google-services") 
}

dependencies {
    
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))    
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
}
```

### 4. 업로드 코드 작성 및 테스트
* app/src/main/java/.../SyncManager.kt 참고

# 업데이트 사항
**수정 날짜: 2026-06-22**

### 1. Social 데이터 수집 모듈 추가
* Call Logs 자동 수집
* 기존 데이터베이스 스키마에 Social 데이터 통합 => 파이어베이스에 함께 업로드됨

### 2. 파이어베이스 계정 및 프로젝트 생성
* Lab 계정으로 파이어베이스 계정 생성
* 프로젝트 생성 후 기존 안드로이드 앱에 연동
* 프로젝트에 관리자를 추가할 수 있어서 추후 우리 각자의 구글 계정으로 모니터링 가능

### 3. 피험자 번호 입력 기능 추가
* 앱 설치와 동시에 해당 피험자 번호를 입력할 수 있도록 기능 추가
* 해당 피험자 번호로 파이어베이스에 폴더 생성되어, 피험자마다 데이터 관리 가능
    * 예) backups/P01/Daily_2026_06_22.csv