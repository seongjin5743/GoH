package com.example.farm.controller;

import com.example.farm.dto.MemberDTO;
import com.example.farm.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final RestTemplate restTemplate;

    private static final String FACE_REGISTER_URL = "http://223.194.128.214:5000/faceRegister";
    private static final String FACE_RECOGNIZE_URL = "http://223.194.128.214:5000/faceRecognize";

    @GetMapping("/farm/save")
    public String saveForm() {
        return "save";
    }
    @PostMapping("/farm/save")
    public String save(@ModelAttribute MemberDTO memberDTO, Model model) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        String passwordRegex = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).{10,}$";

        if (!memberDTO.getMemberEmail().matches(emailRegex)) {
            model.addAttribute("message", "유효한 이메일 형식이 아닙니다.");
            return "save";
        }
        if (!memberDTO.getMemberPassword().matches(passwordRegex)) {
            model.addAttribute("message", "비밀번호는 특수문자 하나 이상을 포함하고 10자 이상이여야합니다.");
            return "save";
        }
        if (memberService.isIdExists(memberDTO.getMemberId())) {
            model.addAttribute("message", "이미 존재하는 id입니다.");
            return "save";
        }
        if (memberService.isEmailExists(memberDTO.getMemberEmail())) {
            model.addAttribute("message", "이미 존재하는 email입니다.");
            return "save";
        }

        memberService.save(memberDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<MemberDTO> request = new HttpEntity<>(memberDTO, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                FACE_REGISTER_URL, HttpMethod.POST, request, String.class);

        return "redirect:" + FACE_REGISTER_URL;
    }

    @GetMapping("/farm/login")
    public String loginForm() {
        return "login";
    }
    @PostMapping("/farm/login")
    public String login(@ModelAttribute MemberDTO memberDTO, Model model) {
        MemberDTO loginResult = memberService.login(memberDTO);
        if (loginResult != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            Map<String, String> loginData = new HashMap<>();
            loginData.put("memberEmail", loginResult.getMemberEmail());
            loginData.put("memberId", loginResult.getMemberId());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(loginData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    FACE_RECOGNIZE_URL, HttpMethod.POST, request, String.class);

            return "redirect:" + FACE_RECOGNIZE_URL;
        } else {
            long remainingLockTime = memberService.getLockTimeRemaining(memberDTO.getMemberId());

            if (remainingLockTime > 0) {
                model.addAttribute("message", "로그인 시도 횟수를 초과했습니다. " + remainingLockTime + "분 후에 다시 시도하세요.");
            } else {
                int remainingAttempts = 5 - memberService.getLoginFailCount(memberDTO.getMemberId());
                model.addAttribute("message", "로그인 시도 횟수가 초과되었습니다. 남은 시도: " + remainingAttempts);
            }
            return "login";
        }
    }

    @GetMapping("/farm/findId")
    public String findIdForm() {
        return "findId";
    }
    @PostMapping("/farm/findId")
    public String findId(@RequestParam("memberEmail") String memberEmail, Model model) {
        boolean isEmailSent = memberService.sendIdByEmail(memberEmail);
        if (isEmailSent) {
            model.addAttribute("message", "아이디가 이메일로 전송되었습니다.");
        } else {
            model.addAttribute("message", "해당 이메일로 등록된 아이디가 없습니다.");
        }
        return "findId";
    }

    @GetMapping("/farm/findPassword")
    public String findPasswordForm() {
        return "findPassword";
    }
    @PostMapping("/farm/findPassword")
    public String findPassword(@RequestParam("memberId") String memberId, @RequestParam("memberEmail") String memberEmail, Model model) {
        boolean isEmailSent = memberService.sendPasswordByEmail(memberId, memberEmail);
        if (isEmailSent) {
            model.addAttribute("message", "비밀번호가 이메일로 전송되었습니다.");
        } else {
            model.addAttribute("message", "해당 정보로 등록된 비밀번호가 없습니다.");
        }
        return "findPassword";
    }
}