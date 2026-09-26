package com.aravind.wavve.di

import javax.inject.Qualifier

/** Marks the OkHttpClient that carries the Piped failover interceptor.
 *  Only inject this into Piped-related network calls — never into podcast,
 *  artwork, or any other request, or the interceptor will rewrite their host too. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PipedHttpClient