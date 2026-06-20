# ieum-android

> 이음(ツナグ) Android 앱 — 가족 소통 기반 치매 조기 징후 탐지 플랫폼

**2026 글로벌 피우다프로젝트** | [이음 서버 레퍼지토리](https://github.com/2026-ieum-project/ieum-server)

---

## 주요 기능

- **음성 편지함 (오늘의 이야기)** — 매일 질문에 음성으로 답하고 가족과 공유
- **함께 보는 사진** — 가족 사진 업로드 + 회상 질문 기반 대화
- **사진 일기 + 추억 타임라인** — 사진·음성/텍스트로 하루 기록, 가족 아카이브
- **두뇌 활력 리포트** — 음성·키스트로크 분석 기반 주간 변화 추이 (자녀 모드)
- **키스트로크 수집 모듈** — TextWatcher 기반 입력 패턴 측정 (ms 단위)

## 기술 스택

| 항목 | 기술 |
|---|---|
| 언어 | Kotlin |
| IDE | Android Studio |
| 인증 / DB | Firebase Auth, Firebase Realtime DB |
| 푸시 알림 | Firebase Cloud Messaging (FCM) |
| 차트 | MPAndroidChart |

## 개발 환경 설정

1. Android Studio 설치 (최신 stable)
2. 레퍼지토리 clone
3. `google-services.json`을 `app/` 폴더에 추가 (팀 채널에서 공유)
4. `local.properties`에 SDK 경로 설정 후 빌드

> `google-services.json`은 `.gitignore`에 포함되어 있으므로 직접 공유 필요

## 기여하기

[CONTRIBUTING.md](./CONTRIBUTING.md) 참고
