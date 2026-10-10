# Health RPG

걸음수(삼성헬스)로 캐릭터를 키우는 헬스 RPG 앱

- `app/` — Android 앱 (Kotlin, Jetpack Compose)
- `server/` — 백엔드 서버 ([server/README.md](server/README.md))

## 삼성헬스 걸음수 읽기 테스트

삼성헬스 → Health Connect 로 동기화된 걸음수를 Health Connect SDK 로 읽는다.

1. 폰에서 **삼성헬스 → 설정 → Health Connect** 연동을 켜고 걸음수 권한 허용
2. Android 13 이하는 Play 스토어에서 Health Connect 설치 (14 이상은 OS 내장)
3. 앱 실행 → "권한 요청" → 걸음수 허용
4. 오늘 걸음수 / 삼성헬스 출처 걸음수 / 최근 7일이 표시되면 성공

관련 코드: `app/src/main/java/com/example/health/health/`

## 앱 → 서버 걸음수 동기화 테스트 (Windows PC + USB 연결 폰)

1. **DB 띄우기**: Docker Desktop 실행 후, 터미널에서
   ```
   cd server
   docker compose up -d
   ```
2. **서버 실행** (처음엔 의존성 다운로드로 몇 분 걸림)
   ```
   gradlew.bat bootRun
   ```
   브라우저에서 http://localhost:8080/api/health 접속 → `{"status":"UP","database":"UP"}` 나오면 OK
3. **폰 → PC 연결** (USB 연결 상태에서, 새 터미널)
   ```
   %LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe reverse tcp:8080 tcp:8080
   ```
   폰의 `127.0.0.1:8080` 이 PC 서버로 연결됨. USB 를 다시 꽂으면 다시 실행해야 함
4. **앱에서** "서버 동기화" 카드 → "연결 확인" → "서버로 동기화"
   → `앱 N걸음 / 서버 저장 N걸음 (일치)` 가 나오면 성공

와이파이로 접속하려면 서버 주소를 `http://<PC IP>:8080` 으로 바꾸고, Windows 방화벽에서 8080 포트를 허용해야 함.
