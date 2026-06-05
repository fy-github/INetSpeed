package com.ikuai.inetspeed.core.iperf3

import com.ikuai.inetspeed.core.iperf3.model.BinaryValidationResult
import com.ikuai.inetspeed.core.iperf3.model.IperfEvent
import com.ikuai.inetspeed.core.iperf3.model.IperfRequest
import com.ikuai.inetspeed.core.iperf3.model.IperfVersion
import com.ikuai.inetspeed.core.iperf3.model.SctpCapability
import kotlinx.coroutines.flow.Flow

interface Iperf3Runner {
    fun run(request: IperfRequest): Flow<IperfEvent>
    fun runCli(testId: String, command: String): Flow<String>
    suspend fun cancel(testId: String)
    suspend fun version(): IperfVersion
    suspend fun validateBinary(): BinaryValidationResult
    suspend fun detectSctp(): SctpCapability
}
