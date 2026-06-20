# 기여 가이드

## 브랜치 전략

```
main        ← 발표/배포용 (PR + 리뷰 1명 필수)
  └── develop  ← 팀 통합 브랜치 (PR 대상)
        ├── feature/기능명
        ├── fix/버그명
        └── chore/작업명
```

예시: `feature/voice-mailbox`, `fix/firebase-auth-token`

## 커밋 컨벤션

```
feat: 음성 편지함 녹음 기능 추가
fix: Firebase 인증 토큰 갱신 오류 수정
chore: .gitignore 항목 추가
docs: README 업데이트
refactor: 키스트로크 모듈 클래스 분리
style: 코드 포맷팅 정리
test: 음성 녹음 유닛 테스트 추가
```

## PR 규칙

1. `develop` 브랜치로 PR 생성
2. PR 제목도 커밋 컨벤션 형식으로
3. 리뷰어 최소 1명 지정
4. `google-services.json`이 커밋에 포함되지 않았는지 확인

## 이슈 규칙

- 작업 시작 전 이슈 먼저 생성
- 커밋/브랜치에 이슈 번호 연결: `feat: 음성 분석 연동 (#5)`
- 라벨 필수 지정
