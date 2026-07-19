package com.akaitigo.labeldecode.security

import io.quarkus.runtime.StartupEvent
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ApiKeyAuthInterceptorUnitTest {
  private fun securityConfig(
      authEnabled: Boolean,
      apiKeys: List<String>?,
  ): SecurityConfig =
      object : SecurityConfig {
        override fun auth(): SecurityConfig.Auth =
            object : SecurityConfig.Auth {
              override fun enabled(): Boolean = authEnabled

              override fun apiKeys(): Optional<List<String>> = Optional.ofNullable(apiKeys)
            }

        override fun rateLimit(): SecurityConfig.RateLimit =
            object : SecurityConfig.RateLimit {
              override fun enabled(): Boolean = false

              override fun requestsPerMinute(): Long = 120

              override fun burstCapacity(): Long = 30
            }
      }

  @Test
  fun `startup fails when auth is enabled without api keys`() {
    val interceptor = ApiKeyAuthInterceptor(securityConfig(authEnabled = true, apiKeys = null))

    val exception =
        assertThrows<IllegalStateException> { interceptor.validateOnStartup(StartupEvent()) }

    assertTrue(exception.message.orEmpty().contains("API_KEYS"))
  }

  @Test
  fun `startup fails when configured keys are blank`() {
    val interceptor =
        ApiKeyAuthInterceptor(securityConfig(authEnabled = true, apiKeys = listOf(" ", "")))

    assertThrows<IllegalStateException> { interceptor.validateOnStartup(StartupEvent()) }
  }

  @Test
  fun `startup succeeds when auth is enabled with api keys`() {
    val interceptor =
        ApiKeyAuthInterceptor(securityConfig(authEnabled = true, apiKeys = listOf("key-1")))

    assertDoesNotThrow { interceptor.validateOnStartup(StartupEvent()) }
  }

  @Test
  fun `startup succeeds when auth is disabled without api keys`() {
    val interceptor = ApiKeyAuthInterceptor(securityConfig(authEnabled = false, apiKeys = null))

    assertDoesNotThrow { interceptor.validateOnStartup(StartupEvent()) }
  }
}
