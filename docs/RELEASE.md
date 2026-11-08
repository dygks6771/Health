# 출시 가이드 (러너스 크로니클)

코드 쪽 출시 준비는 끝난 상태입니다. 아래는 **직접 해야 하는 일**을 순서대로 정리한 것입니다.

## 0. 이미 준비된 것

| 항목 | 상태 |
|---|---|
| 개발용 테스트 기능 제거 | 테스트 러닝·걸음 기록, Health Connect 쓰기 권한, 레이드 초기화 제거 (용병 친구 모집만 유지) |
| 앱 ID | `com.runnerschronicle.app` (`app/build.gradle.kts`, 스토어에 올리면 변경 불가) |
| 출시 빌드 서버 주소 | `gradle.properties` 의 `releaseServerUrl` (https 만 허용, 출시 빌드에는 서버 설정 화면 없음) |
| 서명 | 루트의 `keystore.properties` 가 있으면 release 빌드에 자동 서명 |
| 회원 탈퇴 | 앱 설정 → 회원 탈퇴, 웹 `https://<도메인>/delete-account.html` |
| 개인정보처리방침 | 앱 안(Health Connect 권한 화면·설정·가입 화면), 웹 `https://<도메인>/privacy.html` |
| 보안 | 비밀번호 암호화, 로그인 실패 제한(아이디 15분 5회·IP 20회), 가입 제한(IP 시간당 10회), DB 외부 차단 |
| 알림 | 레벨 업 · 주간 원정 달성 · 주말 저녁 리마인더 (Android 13+ 권한 요청) |
| 서버 배포 파일 | `server/Dockerfile`, `server/deploy/` (PostgreSQL + 서버 + Caddy 자동 HTTPS) |

## 1. 서버를 HTTPS 로 배포

출시 앱은 **https 주소만** 접속합니다. 지금 PC 에 바로 올리는 방법:

1. **무료 도메인**: [duckdns.org](https://www.duckdns.org) 에 로그인 → 이름 만들기 (예: `runners` → `runners.duckdns.org`) → current ip 에 PC 공인 IP (`1.235.34.40`) 입력
2. **방화벽**: 관리자 PowerShell 에서 80·443 포트 열기
   ```powershell
   New-NetFirewallRule -DisplayName "Runners HTTPS" -Direction Inbound -Protocol TCP -LocalPort 80,443 -Action Allow
   ```
3. **실행** (Docker Desktop 켠 상태)
   ```powershell
   cd <프로젝트>\server\deploy
   copy .env.example .env      # 메모장으로 열어 DOMAIN, DB_PASSWORD 채우기
   docker compose -f docker-compose.prod.yml up -d --build
   ```
4. 확인: 브라우저에서 `https://runners.duckdns.org/api/health` → `{"status":"UP","database":"UP"}`
5. 개발용 서버(`gradlew bootRun`, 기존 `docker compose up`)는 끄기 (포트 충돌 방지)

> 장기 운영은 PC 대신 클라우드(예: AWS Lightsail, Oracle Cloud 무료 VM)에 같은 `deploy/` 를 올리는 것을 권장합니다. PC 가 꺼지면 서비스도 멈춥니다.

## 2. 앱 출시 빌드

1. `gradle.properties` 에서 `releaseServerUrl=https://runners.duckdns.org` 로 변경
2. **서명 키 만들기** (한 번만, 잃어버리면 업데이트 불가 → 안전한 곳에 백업)
   Android Studio → Build → Generate Signed App Bundle / APK → Android App Bundle → Create new...
   예: `C:\Users\<사용자>\keys\runners-chronicle.jks`, alias `runners`
3. 프로젝트 루트에 `keystore.properties` 만들기 (저장소에 올라가지 않음)
   ```properties
   storeFile=C:/Users/<사용자>/keys/runners-chronicle.jks
   storePassword=비밀번호
   keyAlias=runners
   keyPassword=비밀번호
   ```
4. Build → Generate Signed App Bundle → release → `app/release/app-release.aab`
5. 폰에서 release APK 로 한 번 더 확인 (로그인 → 동기화 → 원정 → 탈퇴까지)

업데이트할 때마다 `app/build.gradle.kts` 의 `versionCode` 를 1 올리고 `versionName` 을 바꿉니다.

## 3. Google Play Console

1. 개발자 계정 등록 (1회 25달러, 본인 인증)
2. 앱 만들기 → 이름 "러너스 크로니클", 게임, 무료
3. **앱 콘텐츠** 작성
   - 개인정보처리방침 URL: `https://<도메인>/privacy.html`
   - 앱 액세스 권한: 로그인 필요 → 심사용 테스트 계정(아이디/비밀번호) 제공
   - 광고: 없음
   - 콘텐츠 등급 설문: 판타지 폭력(도트 전투) 정도
   - **데이터 보안**: 수집 = 계정 정보(아이디), 건강·피트니스(운동 기록), 앱 활동(게임 진행) / 공유 안 함 / 전송 중 암호화 / 삭제 요청 가능
   - **계정 삭제**: 앱 내 삭제 있음 + 웹 URL `https://<도메인>/delete-account.html`
   - **건강 앱 / Health Connect 권한 신고**: 읽는 데이터 = 운동(ExerciseSession), 거리, 걸음 수 / 용도 = 운동량에 따라 게임 보상 지급 / 백그라운드 읽기 = 앱이 꺼져 있을 때 자동 동기화
4. 내부 테스트 트랙에 AAB 업로드 → 친구 몇 명으로 테스트 → 프로덕션 출시

## 4. 출시 전 최종 점검표

- [ ] 실제 삼성헬스 러닝 기록이 Health Connect 를 거쳐 EXP 로 들어오는지 (밖에서 1km 이상)
- [ ] 앱을 끈 채 다음 날 확인했을 때 자동 동기화·알림이 왔는지
- [ ] 출시 빌드(release)로 로그인·가입·탈퇴
- [ ] 웹 탈퇴 페이지 동작
- [ ] 친구 3~5명 2주 테스트 → 주간 러닝 횟수 변화 기록 (발표 자료로도 활용)
