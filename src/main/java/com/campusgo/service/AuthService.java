package com.campusgo.service;

import com.campusgo.config.JwtUtil;
import com.campusgo.dto.*;
import com.campusgo.entity.*;
import com.campusgo.repository.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final String REQUIRED_DOMAIN = "@mvjce.edu.in";
    private static final Pattern USN_PATTERN = Pattern.compile("^1MJ\\d{2}[A-Z]{2}(?!000)\\d{3}$");
    private static final int COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final OtpRepository otpRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
            StudentRepository studentRepository,
            OtpRepository otpRepository,
            OtpService otpService,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.otpRepository = otpRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public ApiResponse sendOtp(SendOtpRequest req) {
        String email = req.getEmail().trim().toLowerCase();

        if (!email.endsWith(REQUIRED_DOMAIN)) {
            throw new IllegalArgumentException("Must use college email ending with " + REQUIRED_DOMAIN);
        }

        String usn = email.substring(0, email.indexOf("@")).toUpperCase();
        if (!USN_PATTERN.matcher(usn).matches()) {
            throw new IllegalArgumentException("Invalid USN format in email prefix: " + usn);
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        return generateAndSendOtp(email, "CampusGo Verification Code");
    }

    @Transactional
    public ApiResponse sendForgotPasswordOtp(SendOtpRequest req) {
        String email = req.getEmail().trim().toLowerCase();

        if (!email.endsWith(REQUIRED_DOMAIN)) {
            throw new IllegalArgumentException("Must use college email ending with " + REQUIRED_DOMAIN);
        }

        String usn = email.substring(0, email.indexOf("@")).toUpperCase();
        if (!USN_PATTERN.matcher(usn).matches()) {
            throw new IllegalArgumentException("Invalid USN format in email prefix: " + usn);
        }

        if (!userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("No account found with this email");
        }

        return generateAndSendOtp(email, "CampusGo Password Reset Code");
    }

    private ApiResponse generateAndSendOtp(String email, String subject) {
        Optional<Otp> existingOtp = otpRepository.findByEmail(email);

        if (existingOtp.isPresent()) {
            Otp otpObj = existingOtp.get();
            long secondsPassed = Duration.between(otpObj.getLastSentAt(), OffsetDateTime.now()).getSeconds();
            if (secondsPassed < COOLDOWN_SECONDS) {
                long waitTime = COOLDOWN_SECONDS - secondsPassed;
                throw new IllegalArgumentException(
                        "Please wait " + waitTime + " seconds before requesting another OTP.");
            }
        }

        String otpCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(5);

        if (existingOtp.isPresent()) {
            Otp otpObj = existingOtp.get();
            otpObj.setOtpCode(otpCode);
            otpObj.setExpiresAt(expiresAt);
            otpObj.setAttempts(0);
            otpObj.setLastSentAt(OffsetDateTime.now());
            otpRepository.save(otpObj);
        } else {
            otpRepository.save(new Otp(email, otpCode, expiresAt));
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText("Your OTP verification code is: " + otpCode + "\n\nThis code expires in 5 minutes.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class).error("SMTP Error: ", e);
            throw new RuntimeException("Failed to send verification email. Please try again later.");
        }

        return new ApiResponse(true, "OTP sent successfully to " + email);
    }

    @Transactional
    public ApiResponse registerStudent(StudentRegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();

        if (!email.endsWith(REQUIRED_DOMAIN)) {
            throw new IllegalArgumentException("Must use college email ending with " + REQUIRED_DOMAIN);
        }
        String usn = email.substring(0, email.indexOf("@")).toUpperCase();

        Otp otpRecord = otpRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No OTP requested for this email"));

        validateOtpCode(otpRecord, req.getOtp());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (studentRepository.existsByUsn(usn)) {
            throw new IllegalArgumentException("USN " + usn + " is already registered");
        }

        User user = new User(
                req.getName().trim(),
                email,
                passwordEncoder.encode(req.getPassword()),
                Role.STUDENT);
        User savedUser = userRepository.save(user);

        Student student = new Student(usn, savedUser);
        studentRepository.save(student);

        otpRepository.deleteByEmail(email);

        return new ApiResponse(true, "Student registered successfully with USN: " + usn);
    }

    @Transactional
    public ApiResponse resetPassword(ResetPasswordRequest req) {
        String email = req.getEmail().trim().toLowerCase();

        if (!email.endsWith(REQUIRED_DOMAIN)) {
            throw new IllegalArgumentException("Must use college email ending with " + REQUIRED_DOMAIN);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        Otp otpRecord = otpRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No OTP requested for this email"));

        validateOtpCode(otpRecord, req.getOtp());

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        otpRepository.deleteByEmail(email);

        return new ApiResponse(true, "Password reset successfully. You can now log in with your new password.");
    }

    private void validateOtpCode(Otp otpRecord, String submittedOtp) {
        if (OffsetDateTime.now().isAfter(otpRecord.getExpiresAt())) {
            throw new IllegalArgumentException("OTP code has expired. Please request a new one.");
        }

        if (!otpRecord.getOtpCode().equals(submittedOtp)) {
            int remaining = otpService.recordFailedAttemptAndGetRemaining(otpRecord.getEmail());
            if (remaining <= 0) {
                throw new IllegalArgumentException(
                        "Too many failed attempts. OTP has been invalidated. Please request a new one.");
            } else {
                throw new IllegalArgumentException("Invalid OTP code. " + remaining + " attempt(s) remaining.");
            }
        }
    }

    public LoginResponse login(LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name());
    }
}
