ALTER TABLE policy_doc_chunk
  ADD COLUMN search_text LONGTEXT NULL AFTER chunk_text;

UPDATE policy_doc_chunk c
JOIN policy_doc d ON d.id = c.doc_id
SET c.search_text = CONCAT_WS('\n',
  NULLIF(d.title, ''),
  NULLIF(d.category, ''),
  NULLIF(d.version_label, ''),
  NULLIF(d.summary_text, ''),
  NULLIF(d.standard_answer, ''),
  NULLIF(c.chunk_text, '')
);

ALTER TABLE policy_doc_chunk
  MODIFY COLUMN search_text LONGTEXT NOT NULL;

ALTER TABLE policy_doc_chunk
  ADD FULLTEXT KEY fx_policy_doc_chunk_search_text (search_text);
