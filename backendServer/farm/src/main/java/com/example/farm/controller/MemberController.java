package com.example.farm.controller;

import com.example.farm.dto.MemberDTO;
import com.example.farm.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/farm")
public class MemberController {
    private final MemberService memberService;
    private final RestTemplate restTemplate;

    private static final String FACE_REGISTER_URL = "http://223.194.128.214:5000/faceRegister";
    private static final String FACE_RECOGNIZE_URL = "http://223.194.128.214:5000/faceRecognize";

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody MemberDTO memberDTO) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        String passwordRegex = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).{10,}$";

        if (!memberDTO.getMemberEmail().matches(emailRegex)) {
            return ResponseEntity.badRequest().body("유효한 이메일 형식이 아닙니다.");
        }
        if (!memberDTO.getMemberPassword().matches(passwordRegex)) {
            return ResponseEntity.badRequest().body("비밀번호는 특수문자 하나 이상을 포함하고 10자 이상이여야합니다.");
        }
        if (memberService.isIdExists(memberDTO.getMemberId())) {
            return ResponseEntity.badRequest().body("이미 존재하는 id입니다.");
        }
        if (memberService.isEmailExists(memberDTO.getMemberEmail())) {
            return ResponseEntity.badRequest().body("이미 존재하는 email입니다.");
        }

        memberService.save(memberDTO);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<MemberDTO> request = new HttpEntity<>(memberDTO, headers);
            restTemplate.exchange(FACE_REGISTER_URL, HttpMethod.POST, request, String.class);
        } catch (Exception e) {
            System.out.println("외부 얼굴 등록 서버 연결 실패: " + e.getMessage());
        }

        return ResponseEntity.ok("회원가입 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberDTO memberDTO) {
        MemberDTO loginResult = memberService.login(memberDTO);
        if (loginResult != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            Map<String, String> loginData = new HashMap<>();
            loginData.put("memberEmail", loginResult.getMemberEmail());
            loginData.put("memberId", loginResult.getMemberId());

            try {
                HttpEntity<Map<String, String>> request = new HttpEntity<>(loginData, headers);
                restTemplate.exchange(FACE_RECOGNIZE_URL, HttpMethod.POST, request, String.class);
            } catch (Exception e) {
                System.out.println("외부 얼굴 인식 서버 연결 실패: " + e.getMessage());
            }

            return ResponseEntity.ok("로그인 성공");
        } else {
            long remainingLockTime = memberService.getLockTimeRemaining(memberDTO.getMemberId());

            if (remainingLockTime > 0) {
                return ResponseEntity.status(423).body("로그인 시도 횟수를 초과했습니다. " + remainingLockTime + "분 후에 다시 시도하세요.");
            } else {
                int remainingAttempts = 5 - memberService.getLoginFailCount(memberDTO.getMemberId());
                return ResponseEntity.status(401).body("로그인 시도 횟수가 초과되었습니다. 남은 시도: " + remainingAttempts);
            }
        }
    }

    @PostMapping("/findId")
    public ResponseEntity<?> findId(@RequestBody Map<String, String> request) {
        String memberEmail = request.get("memberEmail");
        boolean isEmailSent = memberService.sendIdByEmail(memberEmail);
        if (isEmailSent) {
            return ResponseEntity.ok("아이디가 이메일로 전송되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("해당 이메일로 등록된 아이디가 없습니다.");
        }
    }

    @PostMapping("/findPassword")
    public ResponseEntity<?> findPassword(@RequestBody Map<String, String> request) {
        String memberId = request.get("memberId");
        String memberEmail = request.get("memberEmail");
        boolean isEmailSent = memberService.sendPasswordByEmail(memberId, memberEmail);
        if (isEmailSent) {
            return ResponseEntity.ok("비밀번호가 이메일로 전송되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("해당 정보로 등록된 비밀번호가 없습니다.");
        }
    }
}