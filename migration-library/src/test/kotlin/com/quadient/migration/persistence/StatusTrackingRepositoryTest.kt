package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectName
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Postgres
class StatusTrackingRepositoryTest {
    private val repo = StatusTrackingRepository(ProjectName(aProjectConfig().name))

    @BeforeEach
    fun cleanUp() = repo.deleteAll()

    @Test
    fun `activeIfNotActive tracks untracked resource as active`() {
        val result = repo.activeIfNotActive("id", ResourceType.DocumentObject)

        assertNotNull(result)
        repo.find("id", ResourceType.DocumentObject)!!.statusEvents.shouldBeOfSize(1)
    }

    @Test
    fun `activeIfNotActive does not add another active status to active resource`() {
        repo.active("id", ResourceType.DocumentObject)

        val result = repo.activeIfNotActive("id", ResourceType.DocumentObject)

        assertNull(result)
        repo.find("id", ResourceType.DocumentObject)!!.statusEvents.shouldBeOfSize(1)
    }

    @Test
    fun `activeIfNotActive activates deployed resource`() {
        repo.active("id", ResourceType.Image)
        repo.deployed("id", Uuid.random(), Clock.System.now(), ResourceType.Image, null, InspireOutput.Interactive)

        val result = repo.activeIfNotActive("id", ResourceType.Image)

        assertNotNull(result)
        val events = repo.find("id", ResourceType.Image)!!.statusEvents
        events.shouldBeOfSize(3)
        assertInstanceOf(Deployed::class.java, events[1])
        assertInstanceOf(Active::class.java, events[2])
    }
}
