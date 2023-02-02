cmake_minimum_required (VERSION 2.8.8)
set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}" ${CMAKE_MODULE_PATH})
set(LIBGSASL_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR})
set(LIBGSASL_BINARY_DIR ${CMAKE_CURRENT_BINARY_DIR})

set(SRCS
    ${LIBGSASL_SOURCE_DIR}/gl/gc-gnulib.c
    ${LIBGSASL_SOURCE_DIR}/gl/printf-parse.c
    ${LIBGSASL_SOURCE_DIR}/gl/c-ctype.c
    ${LIBGSASL_SOURCE_DIR}/gl/float.c
    ${LIBGSASL_SOURCE_DIR}/gl/printf-args.c
    ${LIBGSASL_SOURCE_DIR}/gl/hmac-sha1.c
    ${LIBGSASL_SOURCE_DIR}/gl/itold.c
    ${LIBGSASL_SOURCE_DIR}/gl/hmac-md5.c
    ${LIBGSASL_SOURCE_DIR}/gl/gc-pbkdf2-sha1.c
    ${LIBGSASL_SOURCE_DIR}/gl/md5.c
    ${LIBGSASL_SOURCE_DIR}/gl/base64.c
    ${LIBGSASL_SOURCE_DIR}/gl/memxor.c
    ${LIBGSASL_SOURCE_DIR}/gl/sha1.c
    ${LIBGSASL_SOURCE_DIR}/openid20/client.c
    ${LIBGSASL_SOURCE_DIR}/openid20/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/openid20/server.c
    ${LIBGSASL_SOURCE_DIR}/anonymous/client.c
    ${LIBGSASL_SOURCE_DIR}/anonymous/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/anonymous/server.c
    ${LIBGSASL_SOURCE_DIR}/saml20/client.c
    ${LIBGSASL_SOURCE_DIR}/saml20/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/saml20/server.c
    ${LIBGSASL_SOURCE_DIR}/scram/parser.c
    ${LIBGSASL_SOURCE_DIR}/scram/printer.c
    ${LIBGSASL_SOURCE_DIR}/scram/tokens.c
    ${LIBGSASL_SOURCE_DIR}/scram/client.c
    ${LIBGSASL_SOURCE_DIR}/scram/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/scram/server.c
    ${LIBGSASL_SOURCE_DIR}/scram/validate.c
    ${LIBGSASL_SOURCE_DIR}/src/free.c
    ${LIBGSASL_SOURCE_DIR}/src/supportp.c
    ${LIBGSASL_SOURCE_DIR}/src/init.c
    ${LIBGSASL_SOURCE_DIR}/src/mechtools.c
    ${LIBGSASL_SOURCE_DIR}/src/error.c
    ${LIBGSASL_SOURCE_DIR}/src/property.c
    ${LIBGSASL_SOURCE_DIR}/src/done.c
    ${LIBGSASL_SOURCE_DIR}/src/callback.c
    ${LIBGSASL_SOURCE_DIR}/src/xstart.c
    ${LIBGSASL_SOURCE_DIR}/src/xfinish.c
    ${LIBGSASL_SOURCE_DIR}/src/version.c
    ${LIBGSASL_SOURCE_DIR}/src/xstep.c
    ${LIBGSASL_SOURCE_DIR}/src/mechname.c
    ${LIBGSASL_SOURCE_DIR}/src/xcode.c
    ${LIBGSASL_SOURCE_DIR}/src/crypto.c
    ${LIBGSASL_SOURCE_DIR}/src/doxygen.c
    ${LIBGSASL_SOURCE_DIR}/src/suggest.c
    ${LIBGSASL_SOURCE_DIR}/src/saslprep.c
    ${LIBGSASL_SOURCE_DIR}/src/listmech.c
    ${LIBGSASL_SOURCE_DIR}/src/register.c
    ${LIBGSASL_SOURCE_DIR}/src/base64.c
    ${LIBGSASL_SOURCE_DIR}/src/md5pwd.c
    ${LIBGSASL_SOURCE_DIR}/external/client.c
    ${LIBGSASL_SOURCE_DIR}/external/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/external/server.c
    ${LIBGSASL_SOURCE_DIR}/securid/client.c
    ${LIBGSASL_SOURCE_DIR}/securid/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/securid/server.c
    ${LIBGSASL_SOURCE_DIR}/plain/client.c
    ${LIBGSASL_SOURCE_DIR}/plain/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/plain/server.c
    ${LIBGSASL_SOURCE_DIR}/cram-md5/client.c
    ${LIBGSASL_SOURCE_DIR}/cram-md5/challenge.c
    ${LIBGSASL_SOURCE_DIR}/cram-md5/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/cram-md5/server.c
    ${LIBGSASL_SOURCE_DIR}/cram-md5/digest.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/client.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/digesthmac.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/free.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/getsubopt.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/nonascii.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/parser.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/printer.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/qop.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/server.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/session.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/test-parser.c
    ${LIBGSASL_SOURCE_DIR}/digest-md5/validate.c
    ${LIBGSASL_SOURCE_DIR}/login/client.c
    ${LIBGSASL_SOURCE_DIR}/login/mechinfo.c
    ${LIBGSASL_SOURCE_DIR}/login/server.c
)

add_library(libgsasl STATIC ${SRCS})

if (USE_KRB5)
    target_sources(libgsasl PRIVATE
        ${LIBGSASL_SOURCE_DIR}/gssapi/client.c
        ${LIBGSASL_SOURCE_DIR}/gssapi/mechinfo.c
        ${LIBGSASL_SOURCE_DIR}/gssapi/server.c
        )
endif()

target_include_directories(libgsasl PUBLIC ${LIBGSASL_SOURCE_DIR})
target_include_directories(libgsasl PUBLIC ${LIBGSASL_SOURCE_DIR}/gl)
target_include_directories(libgsasl PUBLIC ${LIBGSASL_SOURCE_DIR}/src)
target_include_directories(libgsasl PUBLIC ${LIBGSASL_SOURCE_DIR}/digest-md5)
target_include_directories(libgsasl PUBLIC ${LIBGSASL_SOURCE_DIR}/linux_x86_64/include)
target_include_directories(libgsasl PRIVATE ${LIBGSASL_BINARY_DIR})

if (USE_KRB5)
    target_link_libraries(libgsasl PUBLIC ${KRB5_LIBRARY})
    set(WITH_KRB5 1)
endif()

target_compile_definitions (libgsasl PRIVATE HAVE_CONFIG_H=1)

configure_file(
  "${LIBGSASL_SOURCE_DIR}/linux_x86_64/include/config.h.in"
  "${LIBGSASL_BINARY_DIR}/config.h"
)
