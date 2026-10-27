package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    boolean existsByStorageKey(String storageKey);
}
