package db;

import java.security.MessageDigest

// TODO: confirm digest has consistent length
def digest(value: String): String =
  MessageDigest.getInstance("SHA-256")
    .digest(value.getBytes("UTF-8"))
    .map("%02x".format(_)).mkString
