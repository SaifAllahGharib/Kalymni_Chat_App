package com.kalymni

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.database.FirebaseDatabase
import com.kalymni.databinding.ActivityVerifyPhoneNumberBinding
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class VerifyPhoneNumber : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyPhoneNumberBinding
    private lateinit var codeOne: TextInputLayout
    private lateinit var codeTwo: TextInputLayout
    private lateinit var codeThree: TextInputLayout
    private lateinit var codeFour: TextInputLayout
    private lateinit var codeFive: TextInputLayout
    private lateinit var codeSix: TextInputLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var verificationCode: String = "ssssss"
    private lateinit var resendingToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var prog: ProgressDialog
    private var timeoutSeconds: Long = 60L

    @SuppressLint("SoonBlockedPrivateApi", "DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyPhoneNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        codeOne = binding.codeOne
        codeTwo = binding.codeTwo
        codeThree = binding.codeThree
        codeFour = binding.codeFour
        codeFive = binding.codeFive
        codeSix = binding.codeSix
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        prog = ProgressDialog(this)
        prog.setMessage("Verification in progress")
        prog.setCancelable(false)
        prog.create()

        autoFocusAndVerification()

        binding.numTxt.text = intent.getStringExtra("phone").toString()

        sendOtp(intent.getStringExtra("phone").toString(), false)

        binding.editNum.setOnClickListener {
            startActivity(Intent(this, SignIn::class.java))
            finish()
        }

        binding.resendOtp.setOnClickListener {
            sendOtp(intent.getStringExtra("phone").toString(), true)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun sendOtp(phone: String, isResend: Boolean) {
        startResendTimer()
        val builder = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phone)
            .setTimeout(timeoutSeconds, TimeUnit.SECONDS).setActivity(this)
            .setCallbacks(object : OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {

                }

                override fun onVerificationFailed(firebaseException: FirebaseException) {
                    Toast.makeText(
                        this@VerifyPhoneNumber,
                        firebaseException.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onCodeSent(
                    s: String,
                    forceResendingToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(s, forceResendingToken)
                    verificationCode = s
                    resendingToken = forceResendingToken
                    Toast.makeText(
                        this@VerifyPhoneNumber,
                        "Code send successfully",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        if (isResend) {
            PhoneAuthProvider.verifyPhoneNumber(
                builder.setForceResendingToken(resendingToken).build()
            )
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        }
    }

    private fun startResendTimer() {
        binding.resendOtp.isEnabled = false
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            @SuppressLint("SetTextI18n")
            override fun run() {
                timeoutSeconds--
                runOnUiThread {
                    binding.sec.text = "$timeoutSeconds sec"
                }
                if (timeoutSeconds <= 0) {
                    timeoutSeconds = 60L
                    timer.cancel()
                    runOnUiThread {
                        binding.resendOtp.isEnabled = true
                    }
                }
            }
        }, 0, 1000)
    }

    var code = ""
    private fun autoFocusAndVerification(): String {
        codeOne.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (codeOne.editText!!.text.isEmpty()) {
                    codeOne.editText!!.requestFocus()
                    codeTwo.editText!!.isFocusable = false
                    codeTwo.editText!!.isFocusableInTouchMode = false
                }
                code += checkInputIsNotEmpty(codeOne) {
                    codeTwo.editText!!.isFocusable = true
                    codeTwo.editText!!.isFocusableInTouchMode = true
                    codeTwo.editText!!.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        codeTwo.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (codeTwo.editText!!.text.isEmpty()) {
                    codeOne.editText!!.requestFocus()
                    codeThree.editText!!.isFocusable = false
                    codeThree.editText!!.isFocusableInTouchMode = false
                }
                code += checkInputIsNotEmpty(codeTwo) {
                    codeThree.editText!!.isFocusable = true
                    codeThree.editText!!.isFocusableInTouchMode = true
                    codeThree.editText!!.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        codeThree.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (codeThree.editText!!.text.isEmpty()) {
                    codeTwo.editText!!.requestFocus()
                    codeFour.editText!!.isFocusable = false
                    codeFour.editText!!.isFocusableInTouchMode = false
                }
                code += checkInputIsNotEmpty(codeThree) {
                    codeFour.editText!!.isFocusable = true
                    codeFour.editText!!.isFocusableInTouchMode = true
                    codeFour.editText!!.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        codeFour.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (codeFour.editText!!.text.isEmpty()) {
                    codeThree.editText!!.requestFocus()
                    codeFive.editText!!.isFocusable = false
                    codeFive.editText!!.isFocusableInTouchMode = false
                }
                code += checkInputIsNotEmpty(codeFour) {
                    codeFive.editText!!.isFocusable = true
                    codeFive.editText!!.isFocusableInTouchMode = true
                    codeFive.editText!!.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        codeFive.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (codeFive.editText!!.text.isEmpty()) {
                    codeFour.editText!!.requestFocus()
                    codeSix.editText!!.isFocusable = false
                    codeSix.editText!!.isFocusableInTouchMode = false
                }
                code += checkInputIsNotEmpty(codeFive) {
                    codeSix.editText!!.isFocusable = true
                    codeSix.editText!!.isFocusableInTouchMode = true
                    codeSix.editText!!.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        codeSix.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (codeSix.editText!!.text.isEmpty()) {
                    codeFive.editText!!.requestFocus()
                }
                code += checkInputIsNotEmpty(codeSix) {
                    codeSix.editText!!.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
                }
                if (codeSix.editText!!.text.isNotEmpty()) {
                    if (verificationCode.isNullOrEmpty()) {
                        code = ""
                        val builder =
                            androidx.appcompat.app.AlertDialog.Builder(this@VerifyPhoneNumber)
                        builder.setMessage("The code you entered is incorrect.")
                        builder.setCancelable(false)
                        builder.setPositiveButton("ok") { d, _ ->
                            d.cancel()
                        }
                        val alertDialog = builder.create()
                        alertDialog.show()
                    } else {
                        signIn(PhoneAuthProvider.getCredential(verificationCode, code))
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        return code
    }

    private fun signIn(phoneAuthCredential: PhoneAuthCredential) {
        progress(true)
        auth.signInWithCredential(phoneAuthCredential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                progress(false)
                val i = Intent(this@VerifyPhoneNumber, Status::class.java)
                i.putExtra("phone", intent.getStringExtra("phone").toString())
                startActivity(i)
                finish()
            } else {
                progress(false)
                code = ""
                val builder = AlertDialog.Builder(this)
                builder.setMessage("The code you entered is incorrect.")
                builder.setCancelable(false)
                builder.setPositiveButton("ok") { d, _ ->
                    d.cancel()
                }
                val alertDialog = builder.create()
                alertDialog.show()
            }
        }
    }

    private fun checkInputIsNotEmpty(textInput: TextInputLayout, codeFun: () -> Unit): String {
        var code = ""
        if (textInput.editText!!.text.isNotEmpty()) {
            code += textInput.editText!!.text
            codeFun()
        }
        return code
    }

    private fun progress(isShowing: Boolean) {
        if (isShowing) {
            if (codeOne.editText!!.text.isNotEmpty() && codeTwo.editText!!.text.isNotEmpty() && codeThree.editText!!.text.isNotEmpty() && codeFour.editText!!.text.isNotEmpty() && codeFive.editText!!.text.isNotEmpty() && codeSix.editText!!.text.isNotEmpty()) {
                prog.show()
            }
        } else {
            prog.dismiss()
        }
    }
}