package dev.sabti.alumni_connect.company.entities;

// Platform permission level within a company — distinct from CompanyUserPosition,
// which is the person's actual organizational title and carries no platform permissions.
public enum CompanyRole {
    OWNER,
    RECRUITER,
    MEMBER
}
