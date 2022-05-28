run_basic_analysis() {
  #  $1 = analysis_type
  #  $2 = topology_size
  echo "Running basic $1 analysis with topology of size $2"
  java -jar "target/packet-courier-1.0.jar" "src/test/resources/thorpe/luke/network/simulation/analysis/$1/basic_$1_analysis_size_$2.courierconfig"
}

echo "Compiling Packet Courier"
mvn package --quiet -DskipTests

# Run basic analysis suite.
for analysis_type in "corruption" "drop" "duplication" "latency"
do
  for topology_size in "5" "25" "50" "75" "100"
  do
    run_basic_analysis $analysis_type $topology_size
  done
done
