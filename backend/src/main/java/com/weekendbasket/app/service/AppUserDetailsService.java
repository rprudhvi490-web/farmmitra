package com.weekendbasket.app.service;

import com.weekendbasket.app.model.User;
import com.weekendbasket.app.repository.RoleAccessRepository;
import com.weekendbasket.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleAccessRepository roleAccessRepository;

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + phoneNumber));

        List<SimpleGrantedAuthority> authorities = roleAccessRepository.findByUserId(user.getId())
                .stream()
                .map(ra -> new SimpleGrantedAuthority(ra.getRole().getRoleId()))
                .toList();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getPhoneNumber())
                .password(user.getPassword())
                .authorities(authorities)
                .accountLocked(user.getStatus().equals("BLOCKED"))
                .build();
    }
}
