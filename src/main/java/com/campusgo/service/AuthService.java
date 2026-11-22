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
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final String REQUIRED_DOMAIN = "@mvjce.edu.in";
    private static final Pattern USN_PATTERN = Pattern.compile("^1MJ\\d{2}[A-Z]{2}(?!000)\\d{3}$");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
            StudentRepository studentRepository,
            OtpRepository otpRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.otpRepository = otpRepository;
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

        String otpCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(5);

        Optional<Otp> existingOtp = otpRepository.findByEmail(email);
        if (existingOtp.isPresent()) {
            Otp otpObj = existingOtp.get();
            otpObj.setOtpCode(otpCode);
            otpObj.setExpiresAt(expiresAt);
            otpRepository.save(otpObj);
        } else {
            otpRepository.save(new Otp(email, otpCode, expiresAt));
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("CampusGo Verification Code");
        message.setText("Your OTP verification code for CampusGo registration is: " + otpCode
                + "\n\nThis code expires in 5 minutes.");

        try {
            System.out.println("========== DEV OTP FOR " + email + " IS: " + otpCode + " ==========");
            mailSender.send(message);
        } catch (Exception e) {
            // Log full error in backend terminal for developer
            org.slf4j.LoggerFactory.getLogger(AuthService.class).error("SMTP Error: ", e);

            // Clean generic exception for API client
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

        if (!otpRecord.getOtpCode().equals(req.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP code");
        }

        if (OffsetDateTime.now().isAfter(otpRecord.getExpiresAt())) {
            throw new IllegalArgumentException("OTP code has expired. Please request a new one.");
        }

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
