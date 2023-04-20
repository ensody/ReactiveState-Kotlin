package com.ensody.reactivestate

import cocoapods.OpenSSL_Universal.EVP_MD_CTX_new
import cocoapods.OpenSSL_Universal.EVP_MD_CTX_free

public fun someLinkingTest() {
    val ctx = EVP_MD_CTX_new()
    EVP_MD_CTX_free(ctx)
}
