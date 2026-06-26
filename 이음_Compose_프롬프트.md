# 「이음(ツナグ)」홈 화면 — Jetpack Compose 구현 프롬프트

> 아래 전체를 AI 코딩 도구(Claude Code / Cursor 등)에 그대로 붙여넣으면 됩니다.
> 디자인 시안(조부모·자녀·손자녀 3개 홈 화면)을 1:1로 재현하도록 작성된 프롬프트입니다.

---

## 0. 역할 / 목표

당신은 시니어 친화 가족 소통 앱 **「이음」**의 안드로이드 프론트엔드 개발자입니다.
**Jetpack Compose (Material 3)** 와 **Kotlin** 으로, 사용자 역할에 따라 초기 화면이 달라지는 **3개의 홈 화면**을 구현하세요.

- 조부모 모드 — 큰 글씨·큰 버튼·단순한 흐름
- 자녀 모드 — 소통 기능 + '두뇌 활력 리포트'(분석 참고 정보)
- 손자녀 모드 — 답장 루프 중심 + 추억 타임라인 (분석 기능 없음)

**핵심 원칙**
1. 카카오톡/LINE에 익숙한 시니어를 위해 **메신저형 + 기능 카드** 구조를 기본으로 한다.
2. '치매 검사' 같은 부담스러운 표현은 절대 쓰지 않는다. → **'두뇌 활력', '오늘의 이야기'** 같은 긍정 프레임.
3. 동일한 디자인 토큰(색/타입/모양)을 3개 모드가 공유하되, **조부모 모드만 스케일을 ~1.35배** 키운다.

---

## 1. 디자인 토큰 (`ui/theme/`)

### 1-1. Color.kt
```kotlin
// 배경·표면
val Paper        = Color(0xFFFBF6EF) // 화면 배경(웜 페이퍼)
val Surface      = Color(0xFFFFFFFF) // 카드
val CardBorder   = Color(0xFFF4E9DD) // 카드 테두리
val Line         = Color(0xFFEFE6DA) // 구분선

// 텍스트
val Ink          = Color(0xFF33291F) // 본문/제목
val InkSub       = Color(0xFF8A7F6F) // 보조 텍스트
val Muted         = Color(0xFF9A8E80) // 캡션
val MutedSoft    = Color(0xFFA99B89) // 더 옅은 캡션

// Primary — 소통/답장 (테라코타 코랄)
val Coral        = Color(0xFFE07856)
val CoralDark    = Color(0xFFC85F3F) // 코랄 위 텍스트
val CoralSoft    = Color(0xFFFBEEE6) // 코랄 틴트 배경

// Secondary — 두뇌 활력/건강 (세이지)
val Sage         = Color(0xFF4F8A6B)
val SageDark     = Color(0xFF3F7359)
val SageSoft     = Color(0xFFE9F1EA)
val SageSoft2    = Color(0xFFECF4EE)
val BarTintLo    = Color(0xFFCFE3D6) // 차트 약한 바
val BarTintMid   = Color(0xFFA9CDB6) // 차트 중간 바

// Accent — 알림/포인트 (꿀빛, 소량만)
val Honey        = Color(0xFFE8A23D)
val AlertBg      = Color(0xFFFBF1E5)
val AlertBorder  = Color(0xFFF1DCBE)
val AlertText    = Color(0xFFB07621)
```

### 1-2. Type & Scale
- **폰트: Pretendard** (없으면 Noto Sans KR). Inter/Roboto는 한글이므로 지양.
- 자녀·손자녀(표준) 스케일과 조부모(확대) 스케일을 둔다. `LocalAppScale` CompositionLocal 또는 모드별 Theme로 주입.

| 용도 | 표준(sp/weight) | 조부모 확대(sp/weight) |
|---|---|---|
| 인사 제목 | 22 / 800 | 30 / 800 |
| 질문(히어로) | — | 26 / 800 |
| 카드 제목 | 16–18 / 800 | 19–20 / 800 |
| 본문 | 14–16 / 600 | 18–21 / 600 |
| 주요 버튼 라벨 | 16–17 / 800 | 22–23 / 800 |
| 캡션 | 12–13 / 600 | 16–17 / 600 |
| 하단 탭 라벨 | 11 / 700 | 14 / 800 |
| 하단 탭 아이콘 | 24.dp | 29.dp |

### 1-3. Shape / Spacing / Elevation
```
화면 좌우 패딩 : 표준 16.dp · 조부모 18~24.dp
카드 라운드   : 표준 20~26.dp · 조부모 26~30.dp
버튼 라운드   : 표준 14~16.dp · 조부모 18~22.dp
필 라운드     : 24.dp (가족 그룹 칩, 미읽음 배지 등)
카드 그림자   : 부드럽게. 코랄 카드 = Coral 8~12% / 세이지 카드 = Sage 12% / 일반 = 검정 5~6%
주요 버튼 높이 : 표준 50~54.dp · 조부모 64~76.dp (최소 터치 44.dp 보장)
```

---

## 2. 아키텍처

```
ui/
 ├─ theme/        (Color, Type, Theme, LocalAppScale)
 ├─ component/    (공용 컴포저블)
 ├─ home/
 │   ├─ ElderHomeScreen.kt        // 조부모
 │   ├─ ChildHomeScreen.kt        // 자녀
 │   └─ GrandchildHomeScreen.kt   // 손자녀
 └─ nav/          (AppNavHost, BottomBar)
model/  UserMode, FamilyMember, VoiceMessage, VitalityReport ...
```

```kotlin
enum class UserMode { ELDER, CHILD, GRANDCHILD }
```
- 로그인/가입 시 결정된 `UserMode` 로 진입 화면을 분기한다.
- 모드별로 `MaterialTheme` 의 typography·scale 만 다르게 주입하고 **색/모양 토큰은 공유**한다.
- 화면은 `Scaffold { bottomBar = ModeBottomBar(mode) }` + `LazyColumn`(세로 스크롤) 구조.

---

## 3. 공용 컴포저블 (`ui/component/`)

> 3개 화면이 재사용한다. 모두 `scale` 을 받아 조부모 모드에서 확대되도록 한다.

1. **`IeumStatusBarSpacer`** — 시스템 상태바 높이만큼 `Spacer`(엣지투엣지, `WindowInsets.statusBars`).
2. **`SectionCard`** — `Surface`(color=Surface, shape=RoundedCornerShape, border=CardBorder, 부드러운 그림자) + 내부 패딩. 색/그림자 변형 파라미터.
3. **`PrimaryButton`** — 코랄 채움, 라운드, 흰 텍스트, 선택적 leading 아이콘. 높이 파라미터.
4. **`OutlineButton`** — 코랄/세이지 2px 테두리 + 옅은 틴트 배경 + 진한 텍스트.
5. **`FamilyGroupChip`** — 흰 배경 필 + 테두리, "김씨네 가족".
6. **`NotificationBell`** — 원형 버튼 + 우상단 코랄 점(빨간 점) badge.
7. **`Avatar`** — 원형, 이니셜(예 "영") + 코랄→피치 그라데이션 배경. 사이즈 파라미터.
8. **`VoiceWaveform`** — 막대 15개 가량, 재생 진행 부분은 Coral, 나머지는 `#E9C6B4`. 높이 비율 배열로 그린다.
9. **`MiniBarChart`** — 7개 막대(주간). 최신값 강조: 앞쪽 BarTintLo → BarTintMid → 마지막 Sage. 라운드 상단.
10. **`PhotoPlaceholder`** — 가족 사진 자리. `RoundedCornerShape` 안에 웜톤 그라데이션 + 산/지평선 형태. 실제 이미지는 `Coil` `AsyncImage` 로 교체 가능하게 슬롯화.
11. **`ModeBottomBar(mode)`** — 모드별 탭 구성. 활성=Coral, 비활성=MutedSoft. 라벨+아이콘 세로.

**아이콘**: `material-icons-extended` 또는 커스텀 벡터. 사용 매핑 —
홈=Home(집), 가족 대화=ChatBubbleOutline, 이야기/음성=Mic, 사진=Image/PhotoCamera, 리포트=BarChart, 추억=Schedule(시계)/History, 나=Person, 알림=NotificationsOutlined, 일기=Edit, 답장 보냄=Send.

---

## 4. 화면별 상세 명세

### 4-A. 조부모 모드 — `ElderHomeScreen` (확대 스케일)

배경 `Paper`. 위→아래 `LazyColumn`, item 간격 16.dp, 좌우 패딩 18.dp.

1. **헤더 영역**
   - 한 줄: 왼쪽 `FamilyGroupChip("김씨네 가족")`, 오른쪽 `NotificationBell`(점 표시).
   - 큰 인사: **"영자 할머니,"** 30sp/800, Ink.
   - 보조: **"3월 12일 화요일이에요"** 21sp/600, InkSub.

2. **오늘의 이야기 히어로** — `SectionCard`(라운드 30, 코랄 옅은 그림자)
   - 라벨 행: 코랄 점(11.dp) + **"오늘의 이야기"** 17sp/800 Coral.
   - 질문: **"오늘 점심은\n무엇을 드셨어요?"** 26sp/800 Ink, lineHeight 1.4.
   - **`PrimaryButton`** 풀폭, 높이 76.dp, 라운드 22: 마이크 아이콘(30.dp) + **"눌러서 말하기"** 23sp/800. (음성 녹음 화면으로 이동)
   - 하단 안내: **"손주들이 기다리고 있어요"** 17sp/600 MutedSoft, 가운데 정렬.

3. **가족이 보낸 사진** — `SectionCard`(라운드 30)
   - 제목 **"가족이 사진을 보냈어요"** 19sp/800.
   - `PhotoPlaceholder` 높이 170.dp, 라운드 20. 좌하단에 흰 텍스트 **"손주 지수가 보냈어요"**(그림자).
   - `OutlineButton` 코랄, 풀폭 높이 64.dp: **"답장하기"** 21sp/800.

4. **기능 타일 2열** (`Row` 2칸, 간격 14)
   - 타일1: 코랄 틴트 아이콘 박스(56.dp, 채팅 아이콘) + **"가족 대화"** 20sp/800 + **"새 메시지 2개"** 16sp/700 Coral.
   - 타일2: 세이지 틴트 아이콘 박스(사진 아이콘) + **"오늘 일기"** 20sp/800 + **"사진 한 장"** 16sp/700 Muted.

5. **하단 탭** `ModeBottomBar(ELDER)` — 4개, 큰 사이즈
   - 홈(활성·Coral) / 이야기 / 가족 대화 / 사진. 아이콘 29.dp, 라벨 14sp/800·700.

> **접근성**: 모든 터치 타깃 ≥ 56.dp, `contentDescription` 필수, 대비 AA 이상, 시스템 글꼴 확대 대응(`sp` 사용).

---

### 4-B. 자녀 모드 — `ChildHomeScreen` (표준 스케일)

배경 `Paper`. `LazyColumn`, 좌우 패딩 16.dp, item 간격 14.dp.

1. **헤더** (`Row`, space-between)
   - 왼쪽: "김씨네 가족" 14sp/700 Muted → 아래 **"안녕하세요, 지수님"** 22sp/800 Ink.
   - 오른쪽: 작은 `NotificationBell`(44.dp).

2. **두뇌 활력 리포트 히어로** — `SectionCard`(라운드 26, 세이지 옅은 그림자)
   - 상단 행: 세이지 점 + **"영자 어머니 · 두뇌 활력"** 15sp/800 / 오른쪽 **"이번 주 ›"** 13sp/700 Muted.
   - 본문 `Row`(baseline 정렬):
     - 왼쪽: **"82"** 48sp/800 Sage + "점" + 아래 상승 화살표 + **"지난주보다 +3"** 14sp/800 Sage.
     - 오른쪽 `MiniBarChart` 7바, 높이 64.dp (마지막 바 Sage 강조).
   - **상태 영역(상태에 따라 토글)** ↓
     - 안정: SageSoft 둥근 박스 + 체크 아이콘 + **"이번 주는 평소처럼 안정적이에요"** 15sp/700 SageDark.
     - 조용한 알림(`uiState.alert == true`): AlertBg + AlertBorder 박스 + 경고 아이콘 + **"조용한 알림"** 라벨 + 안내문("최근 소통 패턴에서 평소와 다른 변화가 이어지고 있어요. 필요하시면 상담 안내를 확인해보세요.") + Honey 버튼 **"치매안심센터 안내 보기"**(일본 빌드는 '지역포괄지원센터').
   - ⚠️ '치매 위험', '검사', '진단' 등의 단어 금지. 두뇌 활력 = 음성 유창성 + 입력 유창성 종합 지수(참고 정보)임을 주석으로 명시.

3. **오늘의 활동** — `SectionCard`(라운드 22)
   - 제목 "오늘의 활동" 14sp/800.
   - `Row` 4칸 균등: 음성 / 사진 회상 / 대화 / 일기. 각 칸 = 라운드 박스(46.dp) + 라벨.
     - 참여한 항목 = SageSoft 박스 + Sage 아이콘 + Ink 라벨.
     - 미참여 항목(예: 대화) = `#F3EDE3` 박스 + 흐린 아이콘 + 흐린 라벨. ("최근 며칠간 활동 없음"도 같은 흐린 처리로 표현)

4. **가족 대화 미리보기** — `SectionCard`(라운드 22, 패딩 작게)
   - `Row`: `Avatar("영")` 48.dp + (제목 "가족 대화" 16sp/800 / 오른쪽 시각 "오후 1:20") + 마지막 메시지 1줄 ellipsis "어머니: 점심 잘 먹었다~ …" + 우측 미읽음 배지(코랄 원 "1").

5. **소통 기능 2열** (`Row`)
   - 오늘의 이야기: 코랄 틴트 마이크 + "오늘의 이야기" 15sp/800 + "음성 답변 도착" 13sp/700 Coral.
   - 함께 보는 사진: 세이지 틴트 사진 + "함께 보는 사진" + "사진 보내기" 13sp/700 Muted.

6. **하단 탭** `ModeBottomBar(CHILD)` — 5개: 홈(활성) / 가족 대화 / 리포트 / 추억 / 나.

---

### 4-C. 손자녀 모드 — `GrandchildHomeScreen` (표준 스케일, 분석 UI 없음)

배경 `Paper`. `LazyColumn`, 좌우 패딩 16.dp, item 간격 14.dp.

1. **헤더** — "김씨네 가족" / **"지수님, 안녕하세요"** 22sp/800 + 작은 `NotificationBell`(점).

2. **할머니 음성 메시지 히어로** — `SectionCard`(라운드 26, 배경 `linear-gradient(#FFF6EE→#FFF)`, 테두리 `#F4E2D4`)
   - 상단 `Row`: `Avatar("영")` 48.dp + (이름 "영자 할머니" 16sp/800 / "방금 음성 메시지를 보냈어요" 13sp/700 Coral).
   - **플레이어 바**: 흰 박스(테두리 CardBorder, 라운드 18) 안에 — 코랄 원형 재생 버튼(46.dp, ▶) + `VoiceWaveform`(flex) + 길이 "0:24" 13sp/700 Muted.
   - `PrimaryButton` 풀폭 높이 54.dp: **"답장하기"**. (가족의 답장이 조부모 재접속의 핵심 동력 — 가장 강조)

3. **사진 보내기 프롬프트** — `SectionCard`(라운드 24)
   - 세이지 사진 아이콘 + **"할머니께 옛날 사진 보내기"** 17sp/800.
   - 본문 "사진을 보내면 할머니가 그때 이야기를 음성으로 들려주세요." 14sp/600 InkSub.
   - `OutlineButton` 세이지, 풀폭 높이 50.dp: **"사진 고르기"**.

4. **우리 가족 추억(타임라인)** — 가로 스크롤
   - 헤더 `Row`: "우리 가족 추억" 17sp/800 / "모두 보기 ›" 13sp/700 Muted.
   - `LazyRow`(간격 12, contentPadding 16): 카드 폭 130.dp 항목들 —
     - 사진형: `PhotoPlaceholder` 130×130 라운드 20 + 제목 "바닷가 나들이" + "3월 8일 · 사진".
     - 음성형: CoralSoft 박스 + 코랄 원형 마이크 아이콘 + "할머니 음성" + "3월 12일 · 음성".
     - 일기형: 세이지톤 placeholder + "텃밭 일기" + "3월 10일 · 일기".
   - 시간순으로 음성/사진/일기가 쌓이는 가족 아카이브임을 주석으로 명시.

5. **하단 탭** `ModeBottomBar(GRANDCHILD)` — 5개: 홈(활성) / 가족 대화 / 사진 보내기 / 추억 / 나.

---

## 5. 상태/데이터 (스텁으로 시작, 추후 Firebase 연동)

```kotlin
data class HomeUiState(
    val familyName: String = "김씨네 가족",
    val userName: String,
    val todayQuestion: String = "오늘 점심은 무엇을 드셨어요?",
    val unreadCount: Int = 0,
    // 자녀 모드
    val vitalityScore: Int = 82,
    val vitalityDelta: Int = +3,
    val weeklyBars: List<Float> = listOf(.60f,.48f,.72f,.64f,.80f,.76f,1f),
    val alert: Boolean = false,                  // 조용한 알림 토글
    val todayActivities: Set<Activity> = setOf(Activity.VOICE, Activity.PHOTO, Activity.DIARY),
    // 손자녀 모드
    val grandmaVoiceDuration: String = "0:24",
    val memories: List<MemoryItem> = emptyList()
)
```
- `ViewModel` + `StateFlow<HomeUiState>` 로 노출, `collectAsStateWithLifecycle()` 로 구독.
- **`alert` 플래그**로 자녀 모드 리포트 상태(안정 ↔ 조용한 알림)를 전환 — 시안의 토글과 1:1 대응.
- 향후: 가족 그룹/인증 = Firebase Auth, 메시지/음성/사진 = Firebase Realtime DB + Storage, 두뇌 활력 지수 = 서버(Flask/FastAPI) 응답.

---

## 6. 마무리 요구사항
- 엣지투엣지(`enableEdgeToEdge()`), 상태바/내비게이션바 인셋 처리.
- 모든 텍스트는 **`strings.xml`** 로 분리(한·일 이중 언어 대비, `values-ja` 준비).
- 다크모드는 1차 범위에서 제외(추후), 단 색은 토큰으로 분리해 확장 가능하게.
- `@Preview` 를 3개 화면 모두 작성(조부모 프리뷰는 `fontScale = 1.3f` 포함).
- 코드 스타일: stateless 컴포저블 + state hoisting, 매직넘버는 토큰/dimens로.
```

이 명세대로 `ElderHomeScreen`, `ChildHomeScreen`, `GrandchildHomeScreen` 3개 파일과 공용 컴포넌트, 테마, 바텀바, 더미 ViewModel 까지 생성해 주세요.
