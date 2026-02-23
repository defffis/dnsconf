package com.novibe.dns.next_dns.service;

import com.novibe.common.util.Log;
import com.novibe.dns.next_dns.http.NextDnsDenyClient;
import com.novibe.dns.next_dns.http.NextDnsRateLimitedApiProcessor;
import com.novibe.dns.next_dns.http.dto.request.CreateDenyDto;
import com.novibe.dns.next_dns.http.dto.response.deny.DenyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NextDnsDenyService {

    private final NextDnsDenyClient nextDnsDenyClient;

    public List<String> dropExistingDenys(List<String> newDenyList) {
        Log.io("Fetching existing denylist from NextDNS");
        List<DenyDto> existingDenyList = nextDnsDenyClient.fetchDenylist();
        Set<String> existingDomainsSet = existingDenyList.stream()
                .filter(DenyDto::isActive)
                .map(DenyDto::getId)
                .collect(Collectors.toSet());
        newDenyList.removeIf(existingDomainsSet::contains);
        return newDenyList;
    }



    public void removeExcluded(Set<String> excludedDomains) {
        if (excludedDomains.isEmpty()) return;

        Log.io("Fetching existing denylist from NextDNS");
        List<DenyDto> existing = nextDnsDenyClient.fetchDenylist();
        Predicate<String> excludedMatcher = domain -> excludedDomains.stream()
                .anyMatch(excluded -> domain.equals(excluded) || domain.endsWith("." + excluded));

        List<String> excludedIds = existing.stream()
                .map(DenyDto::getId)
                .filter(excludedMatcher)
                .toList();

        if (excludedIds.isEmpty()) return;

        Log.io("Removing %s excluded denylist domains from NextDNS".formatted(excludedIds.size()));
        NextDnsRateLimitedApiProcessor.callApi(excludedIds, nextDnsDenyClient::deleteDenyById);
    }

    public void saveDenyList(List<String> newDenylist) {
        List<CreateDenyDto> createRequests = newDenylist.stream().map(CreateDenyDto::new).toList();
        Log.io("Saving new denylist to NextDNS...");
        NextDnsRateLimitedApiProcessor.callApi(createRequests, nextDnsDenyClient::saveDeny);
    }

    public void removeAll() {
        Log.io("Fetching existing denylist from NextDNS");
        List<DenyDto> existing = nextDnsDenyClient.fetchDenylist();
        List<String> ids = existing.stream().map(DenyDto::getId).toList();
        Log.io("Removing denylist from NextDNS");
        NextDnsRateLimitedApiProcessor.callApi(ids, nextDnsDenyClient::deleteDenyById);
    }

}
