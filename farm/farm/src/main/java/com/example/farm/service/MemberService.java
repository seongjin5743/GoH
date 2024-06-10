package com.example.farm.service;

import com.example.farm.config.AESUtil;
import com.example.farm.dto.MemberDTO;
import com.example.farm.entity.MemberEntity;
import com.example.farm.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    private final SecretKey secretKey;
    private final AESUtil aesUtil;

    private final JavaMailSender mailSender;
    private static final String FROM_ADDRESS = "kt33439312@gmail.com";

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION = 5;

    public void sendEmail(String Email, String Subject, String Password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(Email);
        message.setFrom(MemberService.FROM_ADDRESS);
        message.setSubject(Subject);
        message.setText(Password);
        mailSender.send(message);
    }

    public boolean sendIdByEmail(String email) {
        return memberRepository.findByMemberEmail(email)
                .map(memberEntity -> {
                    String memberId = memberEntity.getMemberId();
                    sendEmail(email, "회원님의 아이디", "회원님의 아이디는 " + memberId + "입니다.");
                    return true;
                })
                .orElse(false);
    }

    public boolean sendPasswordByEmail(String memberId, String email) {
        return memberRepository.findByMemberIdAndMemberEmail(memberId, email)
                .map(memberEntity -> {
                    try {
                        String encryptedPassword = memberEntity.getMemberPassword();
                        String decryptedPassword = aesUtil.decrypt(encryptedPassword, secretKey);
                        sendEmail(email, "회원님의 비밀번호", "회원님의 비밀번호는 " + decryptedPassword + "입니다.");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    public void save(MemberDTO memberDTO) {
        try {
            String encryptedPassword = aesUtil.encrypt(memberDTO.getMemberPassword(), secretKey);
            memberDTO.setMemberPassword(encryptedPassword);
            MemberEntity memberEntity = MemberEntity.toMemberEntity(memberDTO);
            memberRepository.save(memberEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isIdExists(String Id) {
        return memberRepository.findByMemberId(Id).isPresent();
    }

    public boolean isEmailExists(String Email) {
        return memberRepository.findByMemberEmail(Email).isPresent();
    }

    public MemberDTO login(MemberDTO memberDTO) {
        Optional<MemberEntity> optionalMemberEntity = memberRepository.findByMemberId(memberDTO.getMemberId());

        if (optionalMemberEntity.isPresent()) {
            MemberEntity memberEntity = optionalMemberEntity.get();
            if (memberEntity.getLockUntil() != null && memberEntity.getLockUntil().isAfter(LocalDateTime.now())) {
                return null;
            }
            try {
                String decryptedPassword = aesUtil.decrypt(memberEntity.getMemberPassword(), secretKey);
                if (memberDTO.getMemberPassword().equals(decryptedPassword)) {
                    memberEntity.setLoginFailCount(0);
                    memberEntity.setLockUntil(null);
                    memberRepository.save(memberEntity);
                    return MemberDTO.toMemberDTO(memberEntity);
                } else {
                    handleFailedLoginAttempt(memberEntity);
                }
            } catch (Exception e) {
                e.printStackTrace();
                handleFailedLoginAttempt(memberEntity);
            }
        }
        return null;
    }

    private void handleFailedLoginAttempt(MemberEntity memberEntity) {
        int newFailCount = memberEntity.getLoginFailCount() + 1;
        memberEntity.setLoginFailCount(newFailCount);

        if (newFailCount >= MAX_LOGIN_ATTEMPTS) {
            memberEntity.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION));
        }
        memberRepository.save(memberEntity);
    }

    public long getLockTimeRemaining(String memberId) {
        return memberRepository.findByMemberId(memberId)
                .map(memberEntity -> {
                    if (memberEntity.getLockUntil() != null && memberEntity.getLockUntil().isAfter(LocalDateTime.now())) {
                        return Duration.between(LocalDateTime.now(), memberEntity.getLockUntil()).toMinutes();
                    }
                    return 0L;
                })
                .orElse(0L);
    }

    public int getLoginFailCount(String memberId) {
        return memberRepository.findByMemberId(memberId)
                .map(MemberEntity::getLoginFailCount)
                .orElse(0);
    }
}