#!/bin/bash

# Cross-Platform Testing Script for Artipie
# This script helps test the project on different architectures

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to detect current architecture
detect_architecture() {
    local arch=$(uname -m)
    case $arch in
        x86_64)
            echo "amd64"
            ;;
        arm64|aarch64)
            echo "arm64"
            ;;
        *)
            echo "unknown"
            ;;
    esac
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven first."
        exit 1
    fi
    
    # Check if Java is installed
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 21 first."
        exit 1
    fi
    
    print_success "All prerequisites are met"
}

# Function to run tests
run_tests() {
    local test_type=$1
    local architecture=$2
    
    print_status "Running $test_type tests on $architecture architecture..."
    
    case $test_type in
        "database")
            mvn test -pl artipie-main -Dtest="ArtifactDbTest,DbConsumerTest,SchedulerDbTest" -DfailIfNoTests=false
            ;;
        "all")
            mvn test -pl artipie-main -DfailIfNoTests=false
            ;;
        "quick")
            mvn test -pl artipie-main -Dtest="DbConsumerTest#addsAndRemovesRecord" -DfailIfNoTests=false
            ;;
        *)
            print_error "Unknown test type: $test_type"
            print_status "Available test types: database, all, quick"
            exit 1
            ;;
    esac
}

# Function to show help
show_help() {
    echo "Cross-Platform Testing Script for Artipie"
    echo ""
    echo "Usage: $0 [OPTIONS] [TEST_TYPE]"
    echo ""
    echo "OPTIONS:"
    echo "  -h, --help     Show this help message"
    echo "  -a, --arch     Show current architecture"
    echo "  -c, --check    Check prerequisites only"
    echo ""
    echo "TEST_TYPE:"
    echo "  database       Run database-related tests (default)"
    echo "  all           Run all tests"
    echo "  quick         Run a quick smoke test"
    echo ""
    echo "Examples:"
    echo "  $0                    # Run database tests"
    echo "  $0 all               # Run all tests"
    echo "  $0 quick             # Run quick test"
    echo "  $0 --arch            # Show current architecture"
    echo "  $0 --check           # Check prerequisites"
}

# Main script logic
main() {
    local test_type="database"
    local show_arch=false
    local check_only=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -a|--arch)
                show_arch=true
                shift
                ;;
            -c|--check)
                check_only=true
                shift
                ;;
            database|all|quick)
                test_type=$1
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Show architecture if requested
    if [ "$show_arch" = true ]; then
        local arch=$(detect_architecture)
        print_status "Current architecture: $arch"
        exit 0
    fi
    
    # Check prerequisites
    check_prerequisites
    
    # Exit if only checking prerequisites
    if [ "$check_only" = true ]; then
        exit 0
    fi
    
    # Detect architecture
    local architecture=$(detect_architecture)
    print_status "Detected architecture: $architecture"
    
    # Run tests
    run_tests "$test_type" "$architecture"
    
    print_success "Tests completed successfully on $architecture architecture!"
}

# Run main function with all arguments
main "$@"
