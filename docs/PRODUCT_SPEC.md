# Open Gaz — Product Specification (MVP)

## Audience and launch geography
Drivers in Andijan, Fergana, and Namangan regions. The platform supports CNG, LPG, and other automotive fuel types.

## Core user journey
1. Open the map and view nearby stations.
2. Filter by fuel type and availability.
3. Open a station to inspect status, queue estimate, report count, and freshness.
4. If physically present, submit a status and optional queue report.
5. Review the destination and launch external navigation.

## Availability states
- **OPEN**: sufficient recent, credible positive reports.
- **CLOSED**: sufficient recent, credible negative reports.
- **UNKNOWN**: insufficient, stale, or conflicting evidence.

Status is calculated separately for each station/fuel-type pair. Initial consensus must not treat a fixed number of clicks as proof. The engine considers report freshness, reporter proximity, unique users, rate limits, and conflicting reports. Exact thresholds are configuration and validation decisions.

## Reports
A status report includes station, fuel type, state (open, closed, no fuel, unknown), timestamp, approximate reporter location, and optional note. Queue reports include a non-negative vehicle count and timestamp.

## Queue estimate
Use recent valid reports, robust outlier handling, and a weighted estimate. Always show the number of reports and the age of the newest report. Hide the estimate when evidence is too old or too sparse.

## Safety and privacy
- Request location only when needed and explain why.
- Do not expose a driver's precise report coordinates publicly.
- Do not encourage phone interaction while driving.
- Provide a clear stale-data warning.
- Do not imply station data is officially verified.
- Protect endpoints with authentication, validation, rate limits, and audit logs.

## MVP screens
1. Map and fuel filter
2. Station details
3. Submit status / queue report
4. Profile and contribution history

## Later
Push alerts, moderation tools, station corrections, richer reputation, and operator verification.
