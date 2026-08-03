package com.riloc.app.ui.component.filter

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

class FilterNumber(
    private val value: Int,
    private val minValue: Int = Int.MIN_VALUE,
    private val maxValue: Int = Int.MAX_VALUE,
) : BaseFieldFilter(value.toString()) {

    override fun onFilter(
        inputTextFieldValue: TextFieldValue,
        lastTextFieldValue: TextFieldValue
    ): TextFieldValue {
        return filterInputNumber(inputTextFieldValue, lastTextFieldValue, minValue, maxValue)
    }

    private fun filterInputNumber(
        inputTextFieldValue: TextFieldValue,
        lastInputTextFieldValue: TextFieldValue,
        minValue: Int = Int.MIN_VALUE,
        maxValue: Int = Int.MAX_VALUE,
    ): TextFieldValue {
        val inputString = inputTextFieldValue.text
        lastInputTextFieldValue.text

        val newString = StringBuilder()
        val supportNegative = minValue < 0
        var isNegative = false

        // 鍙厑璁歌礋鍙峰湪棣栦綅锛屽苟涓斿彧鍏佽涓€涓礋鍙?
        if (supportNegative && inputString.isNotEmpty() && inputString.first() == '-') {
            isNegative = true
            newString.append('-')
        }

        for ((i, c) in inputString.withIndex()) {
            if (i == 0 && isNegative) continue // 棣栧瓧绗﹀凡缁忓鐞?
            when (c) {
                in '0'..'9' -> {
                    newString.append(c)
                    // 妫€鏌ユ槸鍚﹁秴鍑鸿寖鍥?
                    val tempText = newString.toString()
                    // 鍙湪涓嶆槸鍗曠嫭 '-' 鏃跺仛鍒ゆ柇锛堝洜涓?'-' toInt 浼氬紓甯革級
                    if (tempText != "-" && tempText.isNotEmpty()) {
                        try {
                            val tempValue = tempText.toInt()
                            if (tempValue !in minValue..maxValue) {
                                newString.deleteCharAt(newString.lastIndex)
                            }
                        } catch (e: NumberFormatException) {
                            // 瓒呭嚭int鑼冨洿
                            newString.deleteCharAt(newString.lastIndex)
                        }
                    }
                }
                // 蹇界暐鍏朵粬瀛楃锛堝寘鎷偣鍙凤級
            }
        }

        val textRange: TextRange
        if (inputTextFieldValue.selection.collapsed) { // 琛ㄧず鐨勬槸鍏夋爣鑼冨洿
            if (inputTextFieldValue.selection.end != inputTextFieldValue.text.length) { // 鍏夋爣娌℃湁鎸囧悜鏈熬
                var newPosition = inputTextFieldValue.selection.end + (newString.length - inputString.length)
                if (newPosition < 0) {
                    newPosition = inputTextFieldValue.selection.end
                }
                textRange = TextRange(newPosition)
            } else { // 鍏夋爣鎸囧悜浜嗘湯灏?
                textRange = TextRange(newString.length)
            }
        } else {
            textRange = TextRange(newString.length)
        }

        return lastInputTextFieldValue.copy(
            text = newString.toString(),
            selection = textRange
        )
    }
}

