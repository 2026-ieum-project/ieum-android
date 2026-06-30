package com.ieum.app.keystroke

class KeystrokeAnalyzer {

    private var lastKeyTime = 0L
    private var sessionStart = 0L
    private var totalChars = 0
    private var correctionStart = 0L
    private var prevLength = 0

    private val ikiList = mutableListOf<Long>()        // 문자 입력 간격
    private val wordPauseList = mutableListOf<Long>()  // 단어 사이 휴지
    private val correctionList = mutableListOf<Long>() // 오타 수정 소요 시간

    fun onTextChanged(newText: String) {
        val now = System.currentTimeMillis()
        val newLength = newText.length

        if (sessionStart == 0L) sessionStart = now

        when {
            newLength > prevLength -> {
                // 글자 추가 (타이핑)
                if (lastKeyTime > 0) ikiList.add(now - lastKeyTime)

                // 수정 후 다시 타이핑 → correction 종료
                if (correctionStart > 0) {
                    correctionList.add(now - correctionStart)
                    correctionStart = 0L
                }

                // 스페이스 입력 = 단어 경계
                if (newText.endsWith(" ") && lastKeyTime > 0) {
                    wordPauseList.add(now - lastKeyTime)
                }

                totalChars++
            }
            newLength < prevLength -> {
                // 글자 삭제 (백스페이스) → correction 시작
                if (correctionStart == 0L) correctionStart = now
            }
        }

        lastKeyTime = now
        prevLength = newLength
    }

    data class Features(
        val interKeyInterval: Double,
        val wordPauseDuration: Double,
        val correctionTime: Double,
        val charsPerMinute: Double
    )

    fun getFeatures(): Features {
        val elapsedMin = if (sessionStart > 0)
            (System.currentTimeMillis() - sessionStart) / 60_000.0
        else 1.0

        return Features(
            interKeyInterval  = if (ikiList.isNotEmpty()) ikiList.average() else 0.0,
            wordPauseDuration = if (wordPauseList.isNotEmpty()) wordPauseList.average() else 0.0,
            correctionTime    = if (correctionList.isNotEmpty()) correctionList.average() else 0.0,
            charsPerMinute    = if (elapsedMin > 0) totalChars / elapsedMin else 0.0
        )
    }

    fun hasData(): Boolean = totalChars > 0

    fun reset() {
        ikiList.clear()
        wordPauseList.clear()
        correctionList.clear()
        totalChars = 0
        lastKeyTime = 0L
        correctionStart = 0L
        sessionStart = 0L
        prevLength = 0
    }
}
