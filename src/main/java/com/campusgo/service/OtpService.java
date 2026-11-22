package com.campusgo.service;

import com.campusgo.entity.Otp;
import com.campusgo.repository.OtpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private static final int MAX_ATTEMPTS = 3;
    private final OtpRepository otpRepository;

    public OtpService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailedAttemptAndGetRemaining(String email) {
        Otp otpRecord = otpRepository.findByEmail(email).orElse(null);
        if (otpRecord == null) {
            return 0;
        }

        int newAttempts = otpRecord.getAttempts() + 1;
        if (newAttempts >= MAX_ATTEMPTS) {
            otpRepository.deleteByEmail(email);
            return 0; // 0 remaining -> OTP invalidated
        } else {
            otpRecord.setAttempts(newAttempts);
            otpRepository.save(otpRecord);
            return MAX_ATTEMPTS - newAttempts;
        }
    }
}
