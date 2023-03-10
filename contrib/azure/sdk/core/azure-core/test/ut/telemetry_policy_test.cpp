// Copyright (c) Microsoft Corporation. All rights reserved.
// SPDX-License-Identifier: MIT

#include <azure/core/http/policies/policy.hpp>
#include <azure/core/internal/http/pipeline.hpp>
#include <gtest/gtest.h>

using namespace Azure::Core;
using namespace Azure::Core::Http;
using namespace Azure::Core::Http::_internal;
using namespace Azure::Core::Http::Policies;
using namespace Azure::Core::Http::Policies::_internal;

namespace {

class NoOpPolicy final : public HttpPolicy {
private:
  std::unique_ptr<RawResponse> Send(
      Request& request,
      NextHttpPolicy nextPolicy,
      Context const& context) const override
  {
    (void)context;
    (void)request;
    (void)nextPolicy;

    return std::unique_ptr<RawResponse>();
  }

  std::unique_ptr<HttpPolicy> Clone() const override { return std::make_unique<NoOpPolicy>(*this); }
};

} // namespace

TEST(TelemetryPolicy, telemetryString)
{
  std::vector<std::unique_ptr<HttpPolicy>> policy1;
  std::vector<std::unique_ptr<HttpPolicy>> policy2;
  std::vector<std::unique_ptr<HttpPolicy>> policy3;
  std::vector<std::unique_ptr<HttpPolicy>> policy4;

  std::string const expected1 = "azsdk-cpp-storage-blob/11.0.0 (";
  policy1.emplace_back(std::make_unique<TelemetryPolicy>("storage-blob", "11.0.0"));
  policy1.emplace_back(std::make_unique<NoOpPolicy>());
  HttpPipeline pipeline1(policy1);

  std::string const expected2 = "AzCopy/10.0.4-Preview azsdk-cpp-storage-blob/11.0.0 (";
  Azure::Core::Http::Policies::TelemetryOptions options2;
  options2.ApplicationId = "AzCopy/10.0.4-Preview";
  policy2.emplace_back(std::make_unique<TelemetryPolicy>("storage-blob", "11.0.0", options2));
  policy2.emplace_back(std::make_unique<NoOpPolicy>());
  HttpPipeline pipeline2(policy2);

  std::string const expected3 = "AzCopy / 10.0.4-Preview azsdk-cpp-storage-blob/11.0.0 (";
  Azure::Core::Http::Policies::TelemetryOptions options3;
  options3.ApplicationId = "  AzCopy / 10.0.4-Preview  ";
  policy3.emplace_back(std::make_unique<TelemetryPolicy>("storage-blob", "11.0.0", options3));
  policy3.emplace_back(std::make_unique<NoOpPolicy>());
  HttpPipeline pipeline3(policy3);

  std::string const expected4 = "01234567890123456789abcd azsdk-cpp-storage-blob/11.0.0 (";
  Azure::Core::Http::Policies::TelemetryOptions options4;
  options4.ApplicationId = "  01234567890123456789abcde  ";
  policy4.emplace_back(std::make_unique<TelemetryPolicy>("storage-blob", "11.0.0", options4));
  policy4.emplace_back(std::make_unique<NoOpPolicy>());
  HttpPipeline pipeline4(policy4);

  constexpr auto TelemetryHeader = "user-agent";
  constexpr auto ClosingBrace = ')';
  constexpr auto OSInfoMin = 10;

  auto request1 = Request(HttpMethod::Get, Url("https://www.microsoft.com"));
  auto request2 = Request(HttpMethod::Get, Url("https://www.microsoft.com"));
  auto request3 = Request(HttpMethod::Get, Url("https://www.microsoft.com"));
  auto request4 = Request(HttpMethod::Get, Url("https://www.microsoft.com"));

  Context context;
  pipeline1.Send(request1, context);
  pipeline2.Send(request2, context);
  pipeline3.Send(request3, context);
  pipeline4.Send(request4, context);

  auto const headers1 = request1.GetHeaders();
  auto const headers2 = request2.GetHeaders();
  auto const headers3 = request3.GetHeaders();
  auto const headers4 = request4.GetHeaders();

  auto telemetryHeader1 = headers1.find(TelemetryHeader);
  auto telemetryHeader2 = headers2.find(TelemetryHeader);
  auto telemetryHeader3 = headers3.find(TelemetryHeader);
  auto telemetryHeader4 = headers4.find(TelemetryHeader);

  EXPECT_NE(telemetryHeader1, headers1.end());
  EXPECT_NE(telemetryHeader2, headers2.end());
  EXPECT_NE(telemetryHeader3, headers3.end());
  EXPECT_NE(telemetryHeader4, headers4.end());

  auto const actualValue1 = telemetryHeader1->second;
  auto const actualValue2 = telemetryHeader2->second;
  auto const actualValue3 = telemetryHeader3->second;
  auto const actualValue4 = telemetryHeader4->second;

  EXPECT_GE(actualValue1.size(), expected1.size() + OSInfoMin + sizeof(ClosingBrace));
  EXPECT_GE(actualValue2.size(), expected2.size() + OSInfoMin + sizeof(ClosingBrace));
  EXPECT_GE(actualValue3.size(), expected3.size() + OSInfoMin + sizeof(ClosingBrace));
  EXPECT_GE(actualValue4.size(), expected4.size() + OSInfoMin + sizeof(ClosingBrace));

  EXPECT_EQ(actualValue1[actualValue1.size() - 1], ClosingBrace);
  EXPECT_EQ(actualValue2[actualValue2.size() - 1], ClosingBrace);
  EXPECT_EQ(actualValue3[actualValue3.size() - 1], ClosingBrace);
  EXPECT_EQ(actualValue4[actualValue4.size() - 1], ClosingBrace);

  EXPECT_EQ(actualValue1.substr(0, expected1.size()), expected1);
  EXPECT_EQ(actualValue2.substr(0, expected2.size()), expected2);
  EXPECT_EQ(actualValue3.substr(0, expected3.size()), expected3);
  EXPECT_EQ(actualValue4.substr(0, expected4.size()), expected4);
}
