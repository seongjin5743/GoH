package com.example.farm.repository;

import com.example.farm.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long>
{
    Optional<MemberEntity> findByMemberId(String memberId);
    Optional<MemberEntity> findByMemberEmail(String email);
    Optional<MemberEntity> findByMemberIdAndMemberEmail(String memberId, String email);
}