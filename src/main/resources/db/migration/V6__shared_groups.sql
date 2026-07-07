-- Groups become shared/multi-user: any accepted 'user' member row in
-- group_members grants that user visibility/manage access to the group
-- (rename/add-member/remove-member), not just the creator. Delete remains
-- creator-only. This index supports the "groups accessible by user" lookup
-- (GroupRepository.findAllAccessibleByUser / GroupService.requireAccessibleGroup).
CREATE INDEX IF NOT EXISTS ix_group_members_user ON group_members(target_type, target_id);
