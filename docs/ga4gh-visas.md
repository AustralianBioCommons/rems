# GA4GH Visas

REMS can produce and consume cryptographically signed GA4GH Visas that
assert a user's access rights.

In the language of the GA4GH specifications, REMS acts as a
[Passport Visa Assertion Repository](https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#passport-visa-assertion-repository),
[Passport Visa Issuer](https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#passport-visa-issuer)
and
[Embedded Token Issuer](https://github.com/ga4gh/data-security/blob/master/AAI/AAIConnectProfile.md#conformance-for-embedded-token-issuers)

More info about GA4GH visas:
- [The GA4GH Passport specification](https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md)
- [The GA4GH Authentication and Authorization Infrastructure specification](https://github.com/ga4gh/data-security/blob/master/AAI/AAIConnectProfile.md)
- [The GA4GH Genomic Data Toolkit](https://www.ga4gh.org/genomic-data-toolkit/)

## Current status

### Producing ControlledAccessGrants Visas

Visa support is experimental and has to be enabled with the `:enable-permissions-api` configuration parameter.

After this, the `/api/permissions` API can be used to query visas for a given user.
[See the API docs in the development environment.](https://rems-dev.2.rahtiapp.fi/swagger-ui/index.html#/permissions).

The API returns a one
[ControlledAccessGrant visa](https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#controlledaccessgrants)
in the
[GA4GH Embedded Token format](https://github.com/ga4gh/data-security/blob/master/AAI/AAIConnectProfile.md#embedded-token-issued-by-embedded-token-issuer)
per each resource the user is entitled to. 

- The Visas are signed with the RSA private key specified in the `:ga4gh-visa-private-key`
configuration parameter. The corresponding public key should be
configured via the `:ga4gh-visa-public-key` parameter. 
- As the specification requires, the Visa headers have a `"jku"` parameter,
that points to the `/api/jwk` url, where the public key can be fetched for verifying the Visa. The base URL is derived from `:public-url` config variable. 
- The `"iss"` is also the same as the `:public-url` config.

### Reading ResearcherStatus Visas

Upon login, REMS fetches an id token from the OIDC server's userinfo
endpoint and parses the
[`ga4gh_passport_v1` claim](https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#passport-claim)
contained in it. If a
[ResearcherStatus Visa](https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#researcherstatus)
is found in the passport, REMS sets the user attribute
`researcher-status-by` to the `by` field of the visa (i.e. `"so"` or
`"system"`). The claim should have an issuer and jku that is configured 
in `:ga4gh-visa-trusted-issuers`, otherwise it will not be trusted. 
See [`config-defaults.edn`](../resources/config-defaults.edn) for details.

If an applicant has `researcher-status-by` with value `"so"` or
`"system"`, REMS shows the handler a "Applicant researcher status"
checkbox in the applicant details.

See also Bona Fide bot in [bots.md](bots.md).
