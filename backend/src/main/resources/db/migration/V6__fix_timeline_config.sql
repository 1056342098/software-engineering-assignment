UPDATE process_timeline_node SET approval_type = 'PARTY_APPLY' WHERE approval_type = 'PARTY';
UPDATE process_timeline_node SET approval_type = 'LEAGUE_APPLY' WHERE approval_type = 'LEAGUE';