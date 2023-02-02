﻿/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

#include <aws/ec2/model/ResourceType.h>
#include <aws/core/utils/HashingUtils.h>
#include <aws/core/Globals.h>
#include <aws/core/utils/EnumParseOverflowContainer.h>

using namespace Aws::Utils;


namespace Aws
{
  namespace EC2
  {
    namespace Model
    {
      namespace ResourceTypeMapper
      {

        static const int client_vpn_endpoint_HASH = HashingUtils::HashString("client-vpn-endpoint");
        static const int customer_gateway_HASH = HashingUtils::HashString("customer-gateway");
        static const int dedicated_host_HASH = HashingUtils::HashString("dedicated-host");
        static const int dhcp_options_HASH = HashingUtils::HashString("dhcp-options");
        static const int egress_only_internet_gateway_HASH = HashingUtils::HashString("egress-only-internet-gateway");
        static const int elastic_ip_HASH = HashingUtils::HashString("elastic-ip");
        static const int elastic_gpu_HASH = HashingUtils::HashString("elastic-gpu");
        static const int export_image_task_HASH = HashingUtils::HashString("export-image-task");
        static const int export_instance_task_HASH = HashingUtils::HashString("export-instance-task");
        static const int fleet_HASH = HashingUtils::HashString("fleet");
        static const int fpga_image_HASH = HashingUtils::HashString("fpga-image");
        static const int host_reservation_HASH = HashingUtils::HashString("host-reservation");
        static const int image_HASH = HashingUtils::HashString("image");
        static const int import_image_task_HASH = HashingUtils::HashString("import-image-task");
        static const int import_snapshot_task_HASH = HashingUtils::HashString("import-snapshot-task");
        static const int instance_HASH = HashingUtils::HashString("instance");
        static const int internet_gateway_HASH = HashingUtils::HashString("internet-gateway");
        static const int key_pair_HASH = HashingUtils::HashString("key-pair");
        static const int launch_template_HASH = HashingUtils::HashString("launch-template");
        static const int local_gateway_route_table_vpc_association_HASH = HashingUtils::HashString("local-gateway-route-table-vpc-association");
        static const int natgateway_HASH = HashingUtils::HashString("natgateway");
        static const int network_acl_HASH = HashingUtils::HashString("network-acl");
        static const int network_interface_HASH = HashingUtils::HashString("network-interface");
        static const int network_insights_analysis_HASH = HashingUtils::HashString("network-insights-analysis");
        static const int network_insights_path_HASH = HashingUtils::HashString("network-insights-path");
        static const int placement_group_HASH = HashingUtils::HashString("placement-group");
        static const int reserved_instances_HASH = HashingUtils::HashString("reserved-instances");
        static const int route_table_HASH = HashingUtils::HashString("route-table");
        static const int security_group_HASH = HashingUtils::HashString("security-group");
        static const int snapshot_HASH = HashingUtils::HashString("snapshot");
        static const int spot_fleet_request_HASH = HashingUtils::HashString("spot-fleet-request");
        static const int spot_instances_request_HASH = HashingUtils::HashString("spot-instances-request");
        static const int subnet_HASH = HashingUtils::HashString("subnet");
        static const int traffic_mirror_filter_HASH = HashingUtils::HashString("traffic-mirror-filter");
        static const int traffic_mirror_session_HASH = HashingUtils::HashString("traffic-mirror-session");
        static const int traffic_mirror_target_HASH = HashingUtils::HashString("traffic-mirror-target");
        static const int transit_gateway_HASH = HashingUtils::HashString("transit-gateway");
        static const int transit_gateway_attachment_HASH = HashingUtils::HashString("transit-gateway-attachment");
        static const int transit_gateway_connect_peer_HASH = HashingUtils::HashString("transit-gateway-connect-peer");
        static const int transit_gateway_multicast_domain_HASH = HashingUtils::HashString("transit-gateway-multicast-domain");
        static const int transit_gateway_route_table_HASH = HashingUtils::HashString("transit-gateway-route-table");
        static const int volume_HASH = HashingUtils::HashString("volume");
        static const int vpc_HASH = HashingUtils::HashString("vpc");
        static const int vpc_peering_connection_HASH = HashingUtils::HashString("vpc-peering-connection");
        static const int vpn_connection_HASH = HashingUtils::HashString("vpn-connection");
        static const int vpn_gateway_HASH = HashingUtils::HashString("vpn-gateway");
        static const int vpc_flow_log_HASH = HashingUtils::HashString("vpc-flow-log");


        ResourceType GetResourceTypeForName(const Aws::String& name)
        {
          int hashCode = HashingUtils::HashString(name.c_str());
          if (hashCode == client_vpn_endpoint_HASH)
          {
            return ResourceType::client_vpn_endpoint;
          }
          else if (hashCode == customer_gateway_HASH)
          {
            return ResourceType::customer_gateway;
          }
          else if (hashCode == dedicated_host_HASH)
          {
            return ResourceType::dedicated_host;
          }
          else if (hashCode == dhcp_options_HASH)
          {
            return ResourceType::dhcp_options;
          }
          else if (hashCode == egress_only_internet_gateway_HASH)
          {
            return ResourceType::egress_only_internet_gateway;
          }
          else if (hashCode == elastic_ip_HASH)
          {
            return ResourceType::elastic_ip;
          }
          else if (hashCode == elastic_gpu_HASH)
          {
            return ResourceType::elastic_gpu;
          }
          else if (hashCode == export_image_task_HASH)
          {
            return ResourceType::export_image_task;
          }
          else if (hashCode == export_instance_task_HASH)
          {
            return ResourceType::export_instance_task;
          }
          else if (hashCode == fleet_HASH)
          {
            return ResourceType::fleet;
          }
          else if (hashCode == fpga_image_HASH)
          {
            return ResourceType::fpga_image;
          }
          else if (hashCode == host_reservation_HASH)
          {
            return ResourceType::host_reservation;
          }
          else if (hashCode == image_HASH)
          {
            return ResourceType::image;
          }
          else if (hashCode == import_image_task_HASH)
          {
            return ResourceType::import_image_task;
          }
          else if (hashCode == import_snapshot_task_HASH)
          {
            return ResourceType::import_snapshot_task;
          }
          else if (hashCode == instance_HASH)
          {
            return ResourceType::instance;
          }
          else if (hashCode == internet_gateway_HASH)
          {
            return ResourceType::internet_gateway;
          }
          else if (hashCode == key_pair_HASH)
          {
            return ResourceType::key_pair;
          }
          else if (hashCode == launch_template_HASH)
          {
            return ResourceType::launch_template;
          }
          else if (hashCode == local_gateway_route_table_vpc_association_HASH)
          {
            return ResourceType::local_gateway_route_table_vpc_association;
          }
          else if (hashCode == natgateway_HASH)
          {
            return ResourceType::natgateway;
          }
          else if (hashCode == network_acl_HASH)
          {
            return ResourceType::network_acl;
          }
          else if (hashCode == network_interface_HASH)
          {
            return ResourceType::network_interface;
          }
          else if (hashCode == network_insights_analysis_HASH)
          {
            return ResourceType::network_insights_analysis;
          }
          else if (hashCode == network_insights_path_HASH)
          {
            return ResourceType::network_insights_path;
          }
          else if (hashCode == placement_group_HASH)
          {
            return ResourceType::placement_group;
          }
          else if (hashCode == reserved_instances_HASH)
          {
            return ResourceType::reserved_instances;
          }
          else if (hashCode == route_table_HASH)
          {
            return ResourceType::route_table;
          }
          else if (hashCode == security_group_HASH)
          {
            return ResourceType::security_group;
          }
          else if (hashCode == snapshot_HASH)
          {
            return ResourceType::snapshot;
          }
          else if (hashCode == spot_fleet_request_HASH)
          {
            return ResourceType::spot_fleet_request;
          }
          else if (hashCode == spot_instances_request_HASH)
          {
            return ResourceType::spot_instances_request;
          }
          else if (hashCode == subnet_HASH)
          {
            return ResourceType::subnet;
          }
          else if (hashCode == traffic_mirror_filter_HASH)
          {
            return ResourceType::traffic_mirror_filter;
          }
          else if (hashCode == traffic_mirror_session_HASH)
          {
            return ResourceType::traffic_mirror_session;
          }
          else if (hashCode == traffic_mirror_target_HASH)
          {
            return ResourceType::traffic_mirror_target;
          }
          else if (hashCode == transit_gateway_HASH)
          {
            return ResourceType::transit_gateway;
          }
          else if (hashCode == transit_gateway_attachment_HASH)
          {
            return ResourceType::transit_gateway_attachment;
          }
          else if (hashCode == transit_gateway_connect_peer_HASH)
          {
            return ResourceType::transit_gateway_connect_peer;
          }
          else if (hashCode == transit_gateway_multicast_domain_HASH)
          {
            return ResourceType::transit_gateway_multicast_domain;
          }
          else if (hashCode == transit_gateway_route_table_HASH)
          {
            return ResourceType::transit_gateway_route_table;
          }
          else if (hashCode == volume_HASH)
          {
            return ResourceType::volume;
          }
          else if (hashCode == vpc_HASH)
          {
            return ResourceType::vpc;
          }
          else if (hashCode == vpc_peering_connection_HASH)
          {
            return ResourceType::vpc_peering_connection;
          }
          else if (hashCode == vpn_connection_HASH)
          {
            return ResourceType::vpn_connection;
          }
          else if (hashCode == vpn_gateway_HASH)
          {
            return ResourceType::vpn_gateway;
          }
          else if (hashCode == vpc_flow_log_HASH)
          {
            return ResourceType::vpc_flow_log;
          }
          EnumParseOverflowContainer* overflowContainer = Aws::GetEnumOverflowContainer();
          if(overflowContainer)
          {
            overflowContainer->StoreOverflow(hashCode, name);
            return static_cast<ResourceType>(hashCode);
          }

          return ResourceType::NOT_SET;
        }

        Aws::String GetNameForResourceType(ResourceType enumValue)
        {
          switch(enumValue)
          {
          case ResourceType::client_vpn_endpoint:
            return "client-vpn-endpoint";
          case ResourceType::customer_gateway:
            return "customer-gateway";
          case ResourceType::dedicated_host:
            return "dedicated-host";
          case ResourceType::dhcp_options:
            return "dhcp-options";
          case ResourceType::egress_only_internet_gateway:
            return "egress-only-internet-gateway";
          case ResourceType::elastic_ip:
            return "elastic-ip";
          case ResourceType::elastic_gpu:
            return "elastic-gpu";
          case ResourceType::export_image_task:
            return "export-image-task";
          case ResourceType::export_instance_task:
            return "export-instance-task";
          case ResourceType::fleet:
            return "fleet";
          case ResourceType::fpga_image:
            return "fpga-image";
          case ResourceType::host_reservation:
            return "host-reservation";
          case ResourceType::image:
            return "image";
          case ResourceType::import_image_task:
            return "import-image-task";
          case ResourceType::import_snapshot_task:
            return "import-snapshot-task";
          case ResourceType::instance:
            return "instance";
          case ResourceType::internet_gateway:
            return "internet-gateway";
          case ResourceType::key_pair:
            return "key-pair";
          case ResourceType::launch_template:
            return "launch-template";
          case ResourceType::local_gateway_route_table_vpc_association:
            return "local-gateway-route-table-vpc-association";
          case ResourceType::natgateway:
            return "natgateway";
          case ResourceType::network_acl:
            return "network-acl";
          case ResourceType::network_interface:
            return "network-interface";
          case ResourceType::network_insights_analysis:
            return "network-insights-analysis";
          case ResourceType::network_insights_path:
            return "network-insights-path";
          case ResourceType::placement_group:
            return "placement-group";
          case ResourceType::reserved_instances:
            return "reserved-instances";
          case ResourceType::route_table:
            return "route-table";
          case ResourceType::security_group:
            return "security-group";
          case ResourceType::snapshot:
            return "snapshot";
          case ResourceType::spot_fleet_request:
            return "spot-fleet-request";
          case ResourceType::spot_instances_request:
            return "spot-instances-request";
          case ResourceType::subnet:
            return "subnet";
          case ResourceType::traffic_mirror_filter:
            return "traffic-mirror-filter";
          case ResourceType::traffic_mirror_session:
            return "traffic-mirror-session";
          case ResourceType::traffic_mirror_target:
            return "traffic-mirror-target";
          case ResourceType::transit_gateway:
            return "transit-gateway";
          case ResourceType::transit_gateway_attachment:
            return "transit-gateway-attachment";
          case ResourceType::transit_gateway_connect_peer:
            return "transit-gateway-connect-peer";
          case ResourceType::transit_gateway_multicast_domain:
            return "transit-gateway-multicast-domain";
          case ResourceType::transit_gateway_route_table:
            return "transit-gateway-route-table";
          case ResourceType::volume:
            return "volume";
          case ResourceType::vpc:
            return "vpc";
          case ResourceType::vpc_peering_connection:
            return "vpc-peering-connection";
          case ResourceType::vpn_connection:
            return "vpn-connection";
          case ResourceType::vpn_gateway:
            return "vpn-gateway";
          case ResourceType::vpc_flow_log:
            return "vpc-flow-log";
          default:
            EnumParseOverflowContainer* overflowContainer = Aws::GetEnumOverflowContainer();
            if(overflowContainer)
            {
              return overflowContainer->RetrieveOverflow(static_cast<int>(enumValue));
            }

            return {};
          }
        }

      } // namespace ResourceTypeMapper
    } // namespace Model
  } // namespace EC2
} // namespace Aws