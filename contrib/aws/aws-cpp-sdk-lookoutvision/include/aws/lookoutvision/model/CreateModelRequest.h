/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

#pragma once
#include <aws/lookoutvision/LookoutforVision_EXPORTS.h>
#include <aws/lookoutvision/LookoutforVisionRequest.h>
#include <aws/core/utils/memory/stl/AWSString.h>
#include <aws/lookoutvision/model/ModelDescription.h>
#include <aws/lookoutvision/model/OutputConfig.h>
#include <utility>
#include <aws/core/utils/UUID.h>

namespace Aws
{
namespace LookoutforVision
{
namespace Model
{

  /**
   */
  class AWS_LOOKOUTFORVISION_API CreateModelRequest : public LookoutforVisionRequest
  {
  public:
    CreateModelRequest();

    // Service request name is the Operation name which will send this request out,
    // each operation should has unique request name, so that we can get operation's name from this request.
    // Note: this is not true for response, multiple operations may have the same response name,
    // so we can not get operation's name from response.
    inline virtual const char* GetServiceRequestName() const override { return "CreateModel"; }

    Aws::String SerializePayload() const override;

    Aws::Http::HeaderValueCollection GetRequestSpecificHeaders() const override;


    /**
     * <p>The name of the project in which you want to create a model version.</p>
     */
    inline const Aws::String& GetProjectName() const{ return m_projectName; }

    /**
     * <p>The name of the project in which you want to create a model version.</p>
     */
    inline bool ProjectNameHasBeenSet() const { return m_projectNameHasBeenSet; }

    /**
     * <p>The name of the project in which you want to create a model version.</p>
     */
    inline void SetProjectName(const Aws::String& value) { m_projectNameHasBeenSet = true; m_projectName = value; }

    /**
     * <p>The name of the project in which you want to create a model version.</p>
     */
    inline void SetProjectName(Aws::String&& value) { m_projectNameHasBeenSet = true; m_projectName = std::move(value); }

    /**
     * <p>The name of the project in which you want to create a model version.</p>
     */
    inline void SetProjectName(const char* value) { m_projectNameHasBeenSet = true; m_projectName.assign(value); }

    /**
     * <p>The name of the project in which you want to create a model version.</p>
     */
    inline CreateModelRequest& WithProjectName(const Aws::String& value) { SetProjectName(value); return *this;}

    /**
     * <p>The name of the project in which you want to create a model version.</p>
     */
    inline CreateModelRequest& WithProjectName(Aws::String&& value) { SetProjectName(std::move(value)); return *this;}

    /**
     * <p>The name of the project in which you want to create a model version.</p>
     */
    inline CreateModelRequest& WithProjectName(const char* value) { SetProjectName(value); return *this;}


    /**
     * <p>A description for the version of the model.</p>
     */
    inline const ModelDescription& GetDescription() const{ return m_description; }

    /**
     * <p>A description for the version of the model.</p>
     */
    inline bool DescriptionHasBeenSet() const { return m_descriptionHasBeenSet; }

    /**
     * <p>A description for the version of the model.</p>
     */
    inline void SetDescription(const ModelDescription& value) { m_descriptionHasBeenSet = true; m_description = value; }

    /**
     * <p>A description for the version of the model.</p>
     */
    inline void SetDescription(ModelDescription&& value) { m_descriptionHasBeenSet = true; m_description = std::move(value); }

    /**
     * <p>A description for the version of the model.</p>
     */
    inline CreateModelRequest& WithDescription(const ModelDescription& value) { SetDescription(value); return *this;}

    /**
     * <p>A description for the version of the model.</p>
     */
    inline CreateModelRequest& WithDescription(ModelDescription&& value) { SetDescription(std::move(value)); return *this;}


    /**
     * <p>ClientToken is an idempotency token that ensures a call to
     * <code>CreateModel</code> completes only once. You choose the value to pass. For
     * example, An issue, such as an network outage, might prevent you from getting a
     * response from <code>CreateModel</code>. In this case, safely retry your call to
     * <code>CreateModel</code> by using the same <code>ClientToken</code> parameter
     * value. An error occurs if the other input parameters are not the same as in the
     * first request. Using a different value for <code>ClientToken</code> is
     * considered a new call to <code>CreateModel</code>. An idempotency token is
     * active for 8 hours.</p>
     */
    inline const Aws::String& GetClientToken() const{ return m_clientToken; }

    /**
     * <p>ClientToken is an idempotency token that ensures a call to
     * <code>CreateModel</code> completes only once. You choose the value to pass. For
     * example, An issue, such as an network outage, might prevent you from getting a
     * response from <code>CreateModel</code>. In this case, safely retry your call to
     * <code>CreateModel</code> by using the same <code>ClientToken</code> parameter
     * value. An error occurs if the other input parameters are not the same as in the
     * first request. Using a different value for <code>ClientToken</code> is
     * considered a new call to <code>CreateModel</code>. An idempotency token is
     * active for 8 hours.</p>
     */
    inline bool ClientTokenHasBeenSet() const { return m_clientTokenHasBeenSet; }

    /**
     * <p>ClientToken is an idempotency token that ensures a call to
     * <code>CreateModel</code> completes only once. You choose the value to pass. For
     * example, An issue, such as an network outage, might prevent you from getting a
     * response from <code>CreateModel</code>. In this case, safely retry your call to
     * <code>CreateModel</code> by using the same <code>ClientToken</code> parameter
     * value. An error occurs if the other input parameters are not the same as in the
     * first request. Using a different value for <code>ClientToken</code> is
     * considered a new call to <code>CreateModel</code>. An idempotency token is
     * active for 8 hours.</p>
     */
    inline void SetClientToken(const Aws::String& value) { m_clientTokenHasBeenSet = true; m_clientToken = value; }

    /**
     * <p>ClientToken is an idempotency token that ensures a call to
     * <code>CreateModel</code> completes only once. You choose the value to pass. For
     * example, An issue, such as an network outage, might prevent you from getting a
     * response from <code>CreateModel</code>. In this case, safely retry your call to
     * <code>CreateModel</code> by using the same <code>ClientToken</code> parameter
     * value. An error occurs if the other input parameters are not the same as in the
     * first request. Using a different value for <code>ClientToken</code> is
     * considered a new call to <code>CreateModel</code>. An idempotency token is
     * active for 8 hours.</p>
     */
    inline void SetClientToken(Aws::String&& value) { m_clientTokenHasBeenSet = true; m_clientToken = std::move(value); }

    /**
     * <p>ClientToken is an idempotency token that ensures a call to
     * <code>CreateModel</code> completes only once. You choose the value to pass. For
     * example, An issue, such as an network outage, might prevent you from getting a
     * response from <code>CreateModel</code>. In this case, safely retry your call to
     * <code>CreateModel</code> by using the same <code>ClientToken</code> parameter
     * value. An error occurs if the other input parameters are not the same as in the
     * first request. Using a different value for <code>ClientToken</code> is
     * considered a new call to <code>CreateModel</code>. An idempotency token is
     * active for 8 hours.</p>
     */
    inline void SetClientToken(const char* value) { m_clientTokenHasBeenSet = true; m_clientToken.assign(value); }

    /**
     * <p>ClientToken is an idempotency token that ensures a call to
     * <code>CreateModel</code> completes only once. You choose the value to pass. For
     * example, An issue, such as an network outage, might prevent you from getting a
     * response from <code>CreateModel</code>. In this case, safely retry your call to
     * <code>CreateModel</code> by using the same <code>ClientToken</code> parameter
     * value. An error occurs if the other input parameters are not the same as in the
     * first request. Using a different value for <code>ClientToken</code> is
     * considered a new call to <code>CreateModel</code>. An idempotency token is
     * active for 8 hours.</p>
     */
    inline CreateModelRequest& WithClientToken(const Aws::String& value) { SetClientToken(value); return *this;}

    /**
     * <p>ClientToken is an idempotency token that ensures a call to
     * <code>CreateModel</code> completes only once. You choose the value to pass. For
     * example, An issue, such as an network outage, might prevent you from getting a
     * response from <code>CreateModel</code>. In this case, safely retry your call to
     * <code>CreateModel</code> by using the same <code>ClientToken</code> parameter
     * value. An error occurs if the other input parameters are not the same as in the
     * first request. Using a different value for <code>ClientToken</code> is
     * considered a new call to <code>CreateModel</code>. An idempotency token is
     * active for 8 hours.</p>
     */
    inline CreateModelRequest& WithClientToken(Aws::String&& value) { SetClientToken(std::move(value)); return *this;}

    /**
     * <p>ClientToken is an idempotency token that ensures a call to
     * <code>CreateModel</code> completes only once. You choose the value to pass. For
     * example, An issue, such as an network outage, might prevent you from getting a
     * response from <code>CreateModel</code>. In this case, safely retry your call to
     * <code>CreateModel</code> by using the same <code>ClientToken</code> parameter
     * value. An error occurs if the other input parameters are not the same as in the
     * first request. Using a different value for <code>ClientToken</code> is
     * considered a new call to <code>CreateModel</code>. An idempotency token is
     * active for 8 hours.</p>
     */
    inline CreateModelRequest& WithClientToken(const char* value) { SetClientToken(value); return *this;}


    /**
     * <p>The location where Amazon Lookout for Vision saves the training results.</p>
     */
    inline const OutputConfig& GetOutputConfig() const{ return m_outputConfig; }

    /**
     * <p>The location where Amazon Lookout for Vision saves the training results.</p>
     */
    inline bool OutputConfigHasBeenSet() const { return m_outputConfigHasBeenSet; }

    /**
     * <p>The location where Amazon Lookout for Vision saves the training results.</p>
     */
    inline void SetOutputConfig(const OutputConfig& value) { m_outputConfigHasBeenSet = true; m_outputConfig = value; }

    /**
     * <p>The location where Amazon Lookout for Vision saves the training results.</p>
     */
    inline void SetOutputConfig(OutputConfig&& value) { m_outputConfigHasBeenSet = true; m_outputConfig = std::move(value); }

    /**
     * <p>The location where Amazon Lookout for Vision saves the training results.</p>
     */
    inline CreateModelRequest& WithOutputConfig(const OutputConfig& value) { SetOutputConfig(value); return *this;}

    /**
     * <p>The location where Amazon Lookout for Vision saves the training results.</p>
     */
    inline CreateModelRequest& WithOutputConfig(OutputConfig&& value) { SetOutputConfig(std::move(value)); return *this;}


    /**
     * <p>The identifier of the AWS Key Management Service (AWS KMS) customer master
     * key (CMK) to use for encypting the model. If this parameter is not specified,
     * the model is encrypted by a key that AWS owns and manages.</p>
     */
    inline const Aws::String& GetKmsKeyId() const{ return m_kmsKeyId; }

    /**
     * <p>The identifier of the AWS Key Management Service (AWS KMS) customer master
     * key (CMK) to use for encypting the model. If this parameter is not specified,
     * the model is encrypted by a key that AWS owns and manages.</p>
     */
    inline bool KmsKeyIdHasBeenSet() const { return m_kmsKeyIdHasBeenSet; }

    /**
     * <p>The identifier of the AWS Key Management Service (AWS KMS) customer master
     * key (CMK) to use for encypting the model. If this parameter is not specified,
     * the model is encrypted by a key that AWS owns and manages.</p>
     */
    inline void SetKmsKeyId(const Aws::String& value) { m_kmsKeyIdHasBeenSet = true; m_kmsKeyId = value; }

    /**
     * <p>The identifier of the AWS Key Management Service (AWS KMS) customer master
     * key (CMK) to use for encypting the model. If this parameter is not specified,
     * the model is encrypted by a key that AWS owns and manages.</p>
     */
    inline void SetKmsKeyId(Aws::String&& value) { m_kmsKeyIdHasBeenSet = true; m_kmsKeyId = std::move(value); }

    /**
     * <p>The identifier of the AWS Key Management Service (AWS KMS) customer master
     * key (CMK) to use for encypting the model. If this parameter is not specified,
     * the model is encrypted by a key that AWS owns and manages.</p>
     */
    inline void SetKmsKeyId(const char* value) { m_kmsKeyIdHasBeenSet = true; m_kmsKeyId.assign(value); }

    /**
     * <p>The identifier of the AWS Key Management Service (AWS KMS) customer master
     * key (CMK) to use for encypting the model. If this parameter is not specified,
     * the model is encrypted by a key that AWS owns and manages.</p>
     */
    inline CreateModelRequest& WithKmsKeyId(const Aws::String& value) { SetKmsKeyId(value); return *this;}

    /**
     * <p>The identifier of the AWS Key Management Service (AWS KMS) customer master
     * key (CMK) to use for encypting the model. If this parameter is not specified,
     * the model is encrypted by a key that AWS owns and manages.</p>
     */
    inline CreateModelRequest& WithKmsKeyId(Aws::String&& value) { SetKmsKeyId(std::move(value)); return *this;}

    /**
     * <p>The identifier of the AWS Key Management Service (AWS KMS) customer master
     * key (CMK) to use for encypting the model. If this parameter is not specified,
     * the model is encrypted by a key that AWS owns and manages.</p>
     */
    inline CreateModelRequest& WithKmsKeyId(const char* value) { SetKmsKeyId(value); return *this;}

  private:

    Aws::String m_projectName;
    bool m_projectNameHasBeenSet;

    ModelDescription m_description;
    bool m_descriptionHasBeenSet;

    Aws::String m_clientToken;
    bool m_clientTokenHasBeenSet;

    OutputConfig m_outputConfig;
    bool m_outputConfigHasBeenSet;

    Aws::String m_kmsKeyId;
    bool m_kmsKeyIdHasBeenSet;
  };

} // namespace Model
} // namespace LookoutforVision
} // namespace Aws
