#include "sentry_testsupport.h"
#include "sentry_value.h"
#include <sentry.h>

SENTRY_TEST(value_null)
{
    sentry_value_t val = sentry_value_new_null();
    TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_NULL);
    TEST_CHECK(sentry_value_is_null(val));
    TEST_CHECK(sentry_value_as_int32(val) == 0);
    TEST_CHECK(!sentry_value_is_true(val));
    TEST_CHECK_JSON_VALUE(val, "null");
    TEST_CHECK(sentry_value_refcount(val) == 1);
    sentry_value_decref(val);
    TEST_CHECK(sentry_value_refcount(val) == 1);
    TEST_CHECK(sentry_value_is_frozen(val));
}

SENTRY_TEST(value_bool)
{
    sentry_value_t val = sentry_value_new_bool(true);
    TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_BOOL);
    TEST_CHECK(sentry_value_as_int32(val) == 0);
    TEST_CHECK(sentry_value_is_true(val));
    TEST_CHECK_JSON_VALUE(val, "true");
    TEST_CHECK(sentry_value_refcount(val) == 1);
    sentry_value_decref(val);
    TEST_CHECK(sentry_value_refcount(val) == 1);
    TEST_CHECK(sentry_value_is_frozen(val));

    val = sentry_value_new_bool(false);
    TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_BOOL);
    TEST_CHECK(sentry_value_as_int32(val) == 0);
    TEST_CHECK(!sentry_value_is_true(val));
    TEST_CHECK_JSON_VALUE(val, "false");
    TEST_CHECK(sentry_value_refcount(val) == 1);
    sentry_value_decref(val);
    TEST_CHECK(sentry_value_refcount(val) == 1);
    TEST_CHECK(sentry_value_is_frozen(val));
}

SENTRY_TEST(value_int32)
{
    sentry_value_t val = sentry_value_new_int32(42);
    TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_INT32);
    TEST_CHECK(sentry_value_as_int32(val) == 42);
    TEST_CHECK(sentry_value_as_double(val) == 42.0);
    TEST_CHECK(sentry_value_is_true(val));
    TEST_CHECK_JSON_VALUE(val, "42");
    TEST_CHECK(sentry_value_refcount(val) == 1);
    sentry_value_decref(val);
    TEST_CHECK(sentry_value_refcount(val) == 1);

    for (int32_t i = -255; i < 255; i++) {
        val = sentry_value_new_int32(i);
        TEST_CHECK_INT_EQUAL((int)i, (int)sentry_value_as_int32(val));
        TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_INT32);
    }

    val = sentry_value_new_int32(-1);
    TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_INT32);
    TEST_CHECK(sentry_value_as_int32(val) == -1);
    TEST_CHECK(sentry_value_is_true(val) == true);
    TEST_CHECK(sentry_value_refcount(val) == 1);
    sentry_value_decref(val);
    TEST_CHECK(sentry_value_refcount(val) == 1);
    TEST_CHECK(sentry_value_is_frozen(val));
}

SENTRY_TEST(value_double)
{
    sentry_value_t val = sentry_value_new_double(42.05);
    TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_DOUBLE);
    TEST_CHECK(sentry_value_as_double(val) == 42.05);
    TEST_CHECK(sentry_value_is_true(val));
    TEST_CHECK_JSON_VALUE(val, "42.05");
    TEST_CHECK(sentry_value_refcount(val) == 1);
    sentry_value_decref(val);
    TEST_CHECK(sentry_value_refcount(val) == 1);
    TEST_CHECK(sentry_value_is_frozen(val));
}

SENTRY_TEST(value_string)
{
    sentry_value_t val = sentry_value_new_string("Hello World!\n\t\r\f");
    TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_STRING);
    TEST_CHECK(sentry_value_is_true(val) == true);
    TEST_CHECK_STRING_EQUAL(
        sentry_value_as_string(val), "Hello World!\n\t\r\f");
    TEST_CHECK_JSON_VALUE(val, "\"Hello World!\\n\\t\\r\\f\"");
    TEST_CHECK(sentry_value_refcount(val) == 1);
    TEST_CHECK(sentry_value_is_frozen(val));
    sentry_value_decref(val);
}

SENTRY_TEST(value_unicode)
{
    // https://xkcd.com/1813/ :-)
    sentry_value_t val
        = sentry_value_new_string("???????????????????? ?????? ????????? \a\v");
    TEST_CHECK_STRING_EQUAL(sentry_value_as_string(val),
        "???????????????????? ?????? ????????? \a\v");
    // json does not need to escape unicode, except for control characters
    TEST_CHECK_JSON_VALUE(
        val, "\"???????????????????? ?????? ????????? \\u0007\\u000b\"");
    sentry_value_decref(val);
    char zalgo[] = "z????????????????????l??????????????????g????????????????o????????????????????????";
    val = sentry_value_new_string(zalgo);
    TEST_CHECK_STRING_EQUAL(sentry_value_as_string(val), zalgo);
    sentry_value_decref(val);
}

SENTRY_TEST(value_list)
{
    sentry_value_t val = sentry_value_new_list();
    for (size_t i = 0; i < 10; i++) {
        TEST_CHECK(
            !sentry_value_append(val, sentry_value_new_int32((int32_t)i)));
    }
    for (size_t i = 0; i < 20; i++) {
        sentry_value_t child = sentry_value_get_by_index(val, i);
        if (i < 10) {
            TEST_CHECK(sentry_value_get_type(child) == SENTRY_VALUE_TYPE_INT32);
            TEST_CHECK(sentry_value_as_int32(child) == (int32_t)i);
        } else {
            TEST_CHECK(sentry_value_is_null(child));
        }
    }
    TEST_CHECK(sentry_value_get_length(val) == 10);
    TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_LIST);
    TEST_CHECK(sentry_value_is_true(val) == true);
    TEST_CHECK_JSON_VALUE(val, "[0,1,2,3,4,5,6,7,8,9]");
    sentry_value_decref(val);

    val = sentry_value_new_list();
    TEST_CHECK(sentry_value_is_true(val) == false);
    TEST_CHECK_JSON_VALUE(val, "[]");
    sentry_value_t copy = sentry__value_clone(val);
    TEST_CHECK_JSON_VALUE(copy, "[]");
    sentry_value_decref(copy);
    sentry_value_decref(val);

    val = sentry_value_new_list();
    sentry_value_set_by_index(val, 5, sentry_value_new_int32(100));
    sentry_value_set_by_index(val, 2, sentry_value_new_int32(10));
    TEST_CHECK_JSON_VALUE(val, "[null,null,10,null,null,100]");
    sentry_value_remove_by_index(val, 2);
    TEST_CHECK_JSON_VALUE(val, "[null,null,null,null,100]");
    TEST_CHECK(!sentry_value_is_frozen(val));
    sentry_value_freeze(val);
    TEST_CHECK(sentry_value_is_frozen(val));
    sentry_value_decref(val);

    val = sentry_value_new_list();
    for (uint32_t i = 1; i <= 10; i++) {
        sentry_value_append(val, sentry_value_new_int32(i));
    }
    sentry__value_append_bounded(val, sentry_value_new_int32(1010), 5);
#define CHECK_IDX(Idx, Val)                                                    \
    TEST_CHECK_INT_EQUAL(                                                      \
        sentry_value_as_int32(sentry_value_get_by_index(val, Idx)), Val)
    CHECK_IDX(0, 7);
    CHECK_IDX(1, 8);
    CHECK_IDX(2, 9);
    CHECK_IDX(3, 10);
    CHECK_IDX(4, 1010);
    sentry_value_decref(val);
}

SENTRY_TEST(value_object)
{
    sentry_value_t val = sentry_value_new_object();
    for (size_t i = 0; i < 10; i++) {
        char key[100];
        sprintf(key, "key%d", (int)i);
        sentry_value_set_by_key(val, key, sentry_value_new_int32((int32_t)i));
    }
    for (size_t i = 0; i < 20; i++) {
        char key[100];
        sprintf(key, "key%d", (int)i);
        sentry_value_t child = sentry_value_get_by_key(val, key);
        if (i < 10) {
            TEST_CHECK(sentry_value_as_int32(child) == (int32_t)i);
        } else {
            TEST_CHECK(sentry_value_is_null(child));
        }
    }

    TEST_CHECK(sentry_value_get_length(val) == 10);
    TEST_CHECK(sentry_value_get_type(val) == SENTRY_VALUE_TYPE_OBJECT);
    TEST_CHECK(sentry_value_is_true(val) == true);
    TEST_CHECK_JSON_VALUE(val,
        "{\"key0\":0,\"key1\":1,\"key2\":2,\"key3\":3,\"key4\":4,\"key5\":5,"
        "\"key6\":6,\"key7\":7,\"key8\":8,\"key9\":9}");

    sentry_value_t val2 = sentry__value_clone(val);
    sentry_value_decref(val);
    val = val2;
    sentry_value_set_by_key(val, "key1", sentry_value_new_int32(100));

    for (size_t i = 0; i < 10; i += 2) {
        char key[100];
        sprintf(key, "key%d", (int)i);
        sentry_value_remove_by_key(val, key);
    }

    TEST_CHECK(sentry_value_get_length(val) == 5);
    TEST_CHECK_JSON_VALUE(
        val, "{\"key1\":100,\"key3\":3,\"key5\":5,\"key7\":7,\"key9\":9}");

    sentry_value_decref(val);

    val = sentry_value_new_object();
    TEST_CHECK(sentry_value_is_true(val) == false);
    TEST_CHECK_JSON_VALUE(val, "{}");
    TEST_CHECK(!sentry_value_is_frozen(val));
    sentry_value_freeze(val);
    TEST_CHECK(sentry_value_is_frozen(val));
    sentry_value_decref(val);
}

SENTRY_TEST(value_freezing)
{
    sentry_value_t val = sentry_value_new_list();
    sentry_value_t inner = sentry_value_new_object();
    sentry_value_append(val, inner);
    TEST_CHECK(!sentry_value_is_frozen(val));
    TEST_CHECK(!sentry_value_is_frozen(inner));
    sentry_value_freeze(val);
    TEST_CHECK(sentry_value_is_frozen(val));
    TEST_CHECK(sentry_value_is_frozen(inner));

    TEST_CHECK_INT_EQUAL(sentry_value_append(val, sentry_value_new_bool(1)), 1);
    TEST_CHECK_INT_EQUAL(sentry_value_get_length(val), 1);

    TEST_CHECK_INT_EQUAL(
        sentry_value_set_by_key(inner, "foo", sentry_value_new_bool(1)), 1);
    TEST_CHECK_INT_EQUAL(sentry_value_get_length(inner), 0);

    sentry_value_decref(val);
}

#define STRING(X) X, (sizeof(X) - 1)

SENTRY_TEST(value_json_parsing)
{
    sentry_value_t rv = sentry__value_from_json(STRING("[42, \"foo\\u2603\"]"));
    TEST_CHECK_INT_EQUAL(
        sentry_value_as_int32(sentry_value_get_by_index(rv, 0)), 42);
    TEST_CHECK_STRING_EQUAL(
        sentry_value_as_string(sentry_value_get_by_index(rv, 1)),
        "foo\xe2\x98\x83");
    sentry_value_decref(rv);

    rv = sentry__value_from_json(
        STRING("[false, 42, \"foo\\u2603\", \"bar\", {\"foo\": 42}]"));
    TEST_CHECK_JSON_VALUE(rv, "[false,42,\"foo???\",\"bar\",{\"foo\":42}]");
    sentry_value_decref(rv);

    rv = sentry__value_from_json(
        STRING("{\"escapes\": "
               "\"quot: \\\", backslash: \\\\, slash: \\/, backspace: \\b, "
               "formfeed: \\f, linefeed: \\n, carriage: \\r, tab: \\t\", "
               "\"surrogates\": "
               "\"\\uD801\\udc37\"}"));
    // escaped forward slashes are parsed, but not generated
    TEST_CHECK_JSON_VALUE(rv,
        "{\"escapes\":"
        "\"quot: \\\", backslash: \\\\, slash: /, backspace: \\b, "
        "formfeed: \\f, linefeed: \\n, carriage: \\r, tab: \\t\","
        "\"surrogates\":\"????\"}");
    sentry_value_decref(rv);

    // unmatched surrogates don???t parse
    rv = sentry__value_from_json(STRING("\"\\uD801\""));
    TEST_CHECK(sentry_value_is_null(rv));
    rv = sentry__value_from_json(
        STRING("{\"valid key\": true, \"invalid key \\uD801\": false}"));
    TEST_CHECK_JSON_VALUE(rv, "{\"valid key\":true}");
    sentry_value_decref(rv);
}

SENTRY_TEST(value_json_escaping)
{
    sentry_value_t rv = sentry__value_from_json(
        STRING("{\"escapes\": "
               "\"quot: \\\", backslash: \\\\, slash: \\/, backspace: \\b, "
               "formfeed: \\f, linefeed: \\n, carriage: \\r, tab: \\t\"}"));
    // escaped forward slashes are parsed, but not generated
    TEST_CHECK_JSON_VALUE(rv,
        "{\"escapes\":"
        "\"quot: \\\", backslash: \\\\, slash: /, backspace: \\b, "
        "formfeed: \\f, linefeed: \\n, carriage: \\r, tab: \\t\"}");
    sentry_value_decref(rv);

    // trailing blackslash
    rv = sentry__value_from_json(STRING("\"\\\""));
    TEST_CHECK(sentry_value_is_null(rv));
}

SENTRY_TEST(value_json_surrogates)
{
    sentry_value_t rv = sentry__value_from_json(
        STRING("{\"surrogates\": \"oh \\uD801\\udc37 hi\"}"));
    TEST_CHECK_JSON_VALUE(rv, "{\"surrogates\":\"oh ???? hi\"}");
    sentry_value_decref(rv);

    // unmatched surrogates don???t parse
    rv = sentry__value_from_json(STRING("\"\\uD801\""));
    TEST_CHECK(sentry_value_is_null(rv));
    rv = sentry__value_from_json(
        STRING("{\"valid key\": true, \"invalid key \\uD801\": false}"));
    TEST_CHECK_JSON_VALUE(rv, "{\"valid key\":true}");
    sentry_value_decref(rv);
}
