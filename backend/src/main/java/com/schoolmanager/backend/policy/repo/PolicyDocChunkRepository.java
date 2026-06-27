package com.schoolmanager.backend.policy.repo;

import com.schoolmanager.backend.policy.entity.PolicyDocChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyDocChunkRepository extends JpaRepository<PolicyDocChunk, Long> {
	@Query(value = """
			SELECT c.id AS chunkId,
			       c.doc_id AS docId,
			       c.chunk_no AS chunkNo,
			       c.chunk_text AS chunkText,
			       MATCH(c.search_text) AGAINST (:q IN NATURAL LANGUAGE MODE) AS score
			FROM policy_doc_chunk c
			JOIN policy_doc d ON d.id = c.doc_id
			WHERE d.status = 'ACTIVE'
			  AND MATCH(c.search_text) AGAINST (:q IN NATURAL LANGUAGE MODE)
			ORDER BY score DESC
			LIMIT 20
			""", nativeQuery = true)
	List<PolicyChunkSearchRow> searchTop(@Param("q") String q);

	@Query("""
			select c from PolicyDocChunk c
			join fetch c.doc d
			where d.status = 'ACTIVE'
			order by d.id desc, c.chunkNo asc
			""")
	List<PolicyDocChunk> findActiveChunksForRetrieval();

	void deleteByDoc_Id(Long docId);

	interface PolicyChunkSearchRow {
		Long getChunkId();

		Long getDocId();

		Integer getChunkNo();

		String getChunkText();

		Double getScore();
	}
}
