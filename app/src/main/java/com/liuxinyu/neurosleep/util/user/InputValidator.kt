package com.liuxinyu.neurosleep.util.user

object InputValidator {
    fun isPhoneValid(phone: String): Boolean {
        return phone.matches(Regex("^\\d{11}$"))
    }

    fun isPasswordValid(password: String): Boolean {
        return password.matches(Regex("^\\S{5,16}$"))
    }
}