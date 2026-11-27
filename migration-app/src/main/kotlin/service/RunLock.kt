package com.quadient.migration.service

import kotlinx.coroutines.sync.Semaphore

class RunLock : Semaphore by Semaphore(1)
