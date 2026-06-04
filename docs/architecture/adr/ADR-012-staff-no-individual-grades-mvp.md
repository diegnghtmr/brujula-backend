# ADR-012: Staff Has No Access to Individual Student Grades in MVP

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

Brújula stores academically sensitive data: students' self-declared course attempts, grades, and academic history. Staff members (administrative personnel) have access to the platform for operational purposes, but their access to individual student records raises privacy and institutional policy concerns.

The question was: in the MVP, should staff be able to view individual students' grade details or only aggregate/anonymized statistics?

Legal and institutional context:
- Habeas Data (Law 1581 of 2012, Colombia): personal academic records are sensitive personal data
- Universidad del Quindío policy: academic records require explicit student consent for third-party access
- All data in Brújula is **autodeclarada** (self-declared by the student) and is NOT official institutional data

---

## Decision

In the MVP, **staff users have no access to individual student grades or course attempt details**. Staff can view only:
- Aggregate statistics (total active users, enrollment counts by program — anonymized)
- Platform configuration (configurable domains, credit limits, program catalog)

Individual student records (`progress`, `curriculum`, `simulation` modules) are accessible only to the owning student.

This is enforced at two levels:
1. **Application layer**: the `staff` role is excluded from use-case ports that return individual student data
2. **API layer**: the OpenAPI contract marks student-data endpoints as requiring the `student` scope; staff tokens do not carry this scope

---

## Consequences

**Positive**:
- Complies with Colombian Habeas Data law and universidad policy without requiring a consent workflow in the MVP
- Reduces surface area of security review for the MVP release
- Simplifies the permission model: student sees own data; staff sees aggregate data; admin configures the system

**Negative**:
- Staff cannot assist individual students with technical issues by viewing their plan (requires a separate "delegate access" feature — post-MVP)
- Aggregate reporting is limited to what can be computed without individual record access

**Neutral**:
- This decision is revisitable in a post-MVP phase if a consent-gating mechanism is implemented
- The `administration` module contains the staff-accessible use cases; it must not import from `progress`, `curriculum`, or `simulation` modules (enforced by `@ApplicationModule(allowedDependencies)`)
