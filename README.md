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
