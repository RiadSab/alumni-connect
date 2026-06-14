package dev.sabti.alumni_connect.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private int jwtExpirationMs;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    private Key key; //Key : interface from java.security for cryptographic keys

    @PostConstruct
    public void init() {
        // Initialize the signing key after the bean is constructed
        // create an HMAC key suitable for the HS256 algorithm
        // the key is created only once when the application starts, and it is reused for signing and verifying JWTs throughout the application's lifecycle.
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email) // I chose email as subject because it's unique for each user and we don't have userName field
                .setIssuer(jwtIssuer)
                .setIssuedAt(now) // Set the token issuance time
                .setExpiration(expiryDate) // Set the token expiration time
                .signWith(key, SignatureAlgorithm.HS256) // Sign the token with the secret key using HS256 algorithm
                // the key is not signed, it's a digital signature of the token, ensuring its integrity and authenticity

                .compact();// Build the JWT and serialize it to a compact, URL-safe string
        //JWT structure:  header.payload.signature
    }

    // Claims: interface from io.jsonwebtoken representing the claims (payload) of a JWT
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()// creates a parser for reading JWT
                .setSigningKey(key) // tells the parser which key to use for verification
                .requireIssuer(jwtIssuer) // validates that the token was issued by us
                .build() // create the final parser instance
                .parseClaimsJws(token) // Parse the JWT and validate its signature
                .getBody(); // Extract the claims (payload) from the token
    }

    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).getSubject(); // Extract the subject (email in this case) from the token's claims
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token).getExpiration(); // Extract the expiration date from the token's claims
    }

    public boolean isTokenExpired(String token) {
        try{
            Date expirationDate = getExpirationDateFromToken(token);
            return expirationDate.before(new Date()); // Check if the token's expiration date is before the current date
        }
        catch(JwtException e){
            logger.error("Invalid JWT token in isTokenExpired: {}", e.getMessage());
            return true; // If there's an error parsing the token, consider it expired/invalid
        }
    }

    public long getTokenValidityDuration(String token){
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            Date now = new Date();
            return expirationDate.getTime() - now.getTime(); // Return the remaining time in milliseconds
        } catch (JwtException e) {
            logger.error("Expired JWT token: {}", e.getMessage());
            return 0; // If there's an error parsing the token, return 0 indicating no validity
        }
    }

    // The outcome of parsing a presented token. EXPIRED is kept distinct from INVALID so the caller
    // can react differently (a future refresh-token flow refreshes on EXPIRED, re-logs-in on INVALID);
    // the security entry point maps these onto the TOKEN_EXPIRED / INVALID_TOKEN ApiError codes.
    public enum TokenStatus { VALID, EXPIRED, INVALID }

    public TokenStatus checkToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(jwtIssuer)
                    .build()
                    .parseClaimsJws(token); // If parsing is successful, the token is valid
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            logger.error("Expired JWT token in checkToken: {}", e.getMessage());
            return TokenStatus.EXPIRED;
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token : {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Malformed JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return TokenStatus.INVALID; // any non-expiry parse failure (malformed, bad signature, etc.)
    }

    public boolean validateToken(String token){
        return checkToken(token) == TokenStatus.VALID;
    }
}
