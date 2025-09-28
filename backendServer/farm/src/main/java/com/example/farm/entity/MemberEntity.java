package com.example.farm.entity;

import com.example.farm.dto.MemberDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@Table(name = "member_table")
public class MemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String memberId;

    @Column
    private String memberEmail;

    @Column
    private String memberPassword;

    @Column
    private String memberName;

    @Column
    private int loginFailCount;

    @Column
    private LocalDateTime lockUntil;

    public static MemberEntity toMemberEntity(MemberDTO memberDTO){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setMemberId(memberDTO.getMemberId());
        memberEntity.setMemberEmail(memberDTO.getMemberEmail());
        memberEntity.setMemberName(memberDTO.getMemberName());
        memberEntity.setMemberPassword(memberDTO.getMemberPassword());
        memberEntity.setLoginFailCount(0);
        memberEntity.setLockUntil(null);
        return memberEntity;
    }
}