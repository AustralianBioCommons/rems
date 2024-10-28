# Linking to REMS

REMS supports linking users directly to application forms, pre-existing applications etc. Unauthenticated users will be redirected to a login page before being sent to the desired url. Below are listed a couple of examples on how the linking works.

## Linking into catalogue

```
https://rems-test.2.rahtiapp.fi/catalogue
```

## Linking into a new application

This creates a draft for an application for catalogue item ids 2 and 3.

```
https://rems-test.2.rahtiapp.fi/application?items=2,3
```

If only the resource ID is known, this will find out which catalogue item matches it and will redirect to the new application page for it.

```
https://rems-test.2.rahtiapp.fi/apply-for?resource=urn:nbn:fi:lb-123456789
```

Multiple resources can also be specified:

```
https://rems-test.2.rahtiapp.fi/apply-for?resource=urn:nbn:fi:lb-123456789&resource=urn:other&resource=urn:third
```

## Linking into an existing application

```
https://rems-test.2.rahtiapp.fi/application/2
```
