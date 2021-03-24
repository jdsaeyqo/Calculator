package com.example.calculator

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.room.Room
import com.example.calculator.model.History
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.history_row.view.*
import java.lang.NumberFormatException

class MainActivity : AppCompatActivity() {

    private var isOperator = false
    private var hasOperator = false

    lateinit var db : AppDataBase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDataBase::class.java,
            "historyDB"
        ).build()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun buttonClicked(view: View) {

        when (view.id) {

            R.id.btn0 -> {
                numberBtnClicked("0")
            }
            R.id.btn1 -> {
                numberBtnClicked("1")
            }
            R.id.btn2 -> {
                numberBtnClicked("2")
            }
            R.id.btn3 -> {
                numberBtnClicked("3")
            }
            R.id.btn4 -> {
                numberBtnClicked("4")
            }
            R.id.btn5 -> {
                numberBtnClicked("5")
            }
            R.id.btn6 -> {
                numberBtnClicked("6")
            }
            R.id.btn7 -> {
                numberBtnClicked("7")
            }
            R.id.btn8 -> {
                numberBtnClicked("8")
            }
            R.id.btn9 -> {
                numberBtnClicked("9")
            }
            R.id.btnPlus -> {
                operatorBtnClicked("+")
            }
            R.id.btnMinus -> {
                operatorBtnClicked("-")
            }
            R.id.btnMul -> {
                operatorBtnClicked("*")
            }
            R.id.btnDiv -> {
                operatorBtnClicked("/")
            }
            R.id.btnPercent -> {
                operatorBtnClicked("%")
            }


        }

    }

    private fun numberBtnClicked(num: String) {

        //숫자를 연산자 다음으로 눌렀을 경우 공백 추가
        if (isOperator) {
            expressionText.append(" ")
        }
        isOperator = false

        val exText = expressionText.text.split(" ")
        if (exText.isNotEmpty() && exText.last().length >= 15) {
            Toast.makeText(this, "15자리 까지만 사용할 수 있습니다", Toast.LENGTH_SHORT).show()
            return
        } else if (exText.last().isEmpty() && num == "0") {
            Toast.makeText(this, "0은 제일 앞에 올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        expressionText.append(num)
        resultText.text = calculate()

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun operatorBtnClicked(operator: String) {

        if (expressionText.text.isEmpty()) {
            return
        }

        when {
            isOperator -> {
                val text = expressionText.text.toString()
                expressionText.text = text.dropLast(1) + operator
            }
            hasOperator -> {
                Toast.makeText(this, "연산자는 한번만 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                expressionText.append(" $operator")
            }
        }

        val ssb = SpannableStringBuilder(expressionText.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            expressionText.text.length - 1,
            expressionText.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        expressionText.text = ssb

        isOperator = true
        hasOperator = true

    }

    fun clearButtonClicked(view: View) {
        expressionText.text = ""
        resultText.text = ""
        isOperator = false
        hasOperator = false


    }

    fun resultButtonClicked(view: View) {
        val exTexts = expressionText.text.split(" ")

        if (expressionText.text.isEmpty() || exTexts.size == 1) {
            return
        }

        if (exTexts.size != 3 && hasOperator) {
            Toast.makeText(this, "아직 완성되지 않은 수식입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (exTexts[0].isNumber().not() || exTexts[2].isNumber().not()) {
            Toast.makeText(this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val exText = expressionText.text.toString()

        val resultTxt = calculate()

        Thread(Runnable {
            db.historyDao().insertHistory(History(null,exText,resultTxt))
        }).start()

        resultText.text = ""
        expressionText.text = resultTxt



        isOperator = false
        hasOperator = false


    }

    private fun calculate(): String {

        val exTexts = expressionText.text.split(" ")
        if (hasOperator.not() || exTexts.size != 3) {
            return ""
        } else if (exTexts[0].isNumber().not() || exTexts[2].isNumber().not()) {
            return ""
        }
        val exp1 = exTexts[0].toBigInteger()
        val exp2 = exTexts[2].toBigInteger()
        val op = exTexts[1]

        return when (op) {
            "+" -> {
                (exp1 + exp2).toString()
            }
            "-" -> {
                (exp1 - exp2).toString()
            }
            "*" -> {
                (exp1 * exp2).toString()
            }
            "/" -> {
                (exp1 / exp2).toString()
            }
            "%" -> {
                (exp1 % exp2).toString()
            }
            else -> ""

        }

    }

    fun historyButtonClicked(view: View) {
        historyLayout.isVisible = true

        historyLinearLayout.removeAllViews()

       Thread(Runnable {

           db.historyDao().getAll().reversed().forEach {

                runOnUiThread {
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_row,null,false)
                    historyView.expressionText.text = it.expression
                    historyView.resultText.text = "= ${it.result}"

                    historyLinearLayout.addView(historyView)

                }

           }

       }).start()
    }

    fun closeHisoryButtonClicked(view: View) {

        historyLayout.isVisible = false

    }

    fun clearHisoryButtonClicked(view: View) {

        historyLinearLayout.removeAllViews()

        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()

    }
}

private fun String.isNumber(): Boolean {
    return try {
        this.toBigInteger()
        true

    } catch (e: NumberFormatException) {
        false
    }
}
