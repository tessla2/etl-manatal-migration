package com.migration.manatal.repository;

import com.migration.manatal.entity.StoredAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredAttachmentRepository extends JpaRepository<StoredAttachment, Long> {
}
